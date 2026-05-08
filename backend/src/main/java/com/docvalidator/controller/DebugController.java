package com.docvalidator.controller;

import com.docvalidator.config.DocValidatorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Debug controller to check configuration and environment
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    
    private final DocValidatorConfig config;
    
    public DebugController(DocValidatorConfig config) {
        this.config = config;
    }
    
    /**
     * Check environment variables and configuration
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> checkConfig() {
        Map<String, Object> status = new HashMap<>();
        
        // Check environment variables
        Map<String, String> envVars = new HashMap<>();
        envVars.put("OPENROUTER_API_KEY", maskKey(System.getenv("OPENROUTER_API_KEY")));
        envVars.put("SPOTIFY_CLIENT_ID", maskKey(System.getenv("SPOTIFY_CLIENT_ID")));
        envVars.put("SPOTIFY_CLIENT_SECRET", maskKey(System.getenv("SPOTIFY_CLIENT_SECRET")));
        
        // Check configuration
        Map<String, Object> configStatus = new HashMap<>();
        configStatus.put("targetApi.baseUrl", config.getTargetApi().getBaseUrl());
        configStatus.put("targetApi.openapiSpecUrl", config.getTargetApi().getOpenapiSpecUrl());
        configStatus.put("ai.provider", config.getAi().getProvider());
        configStatus.put("ai.openai.apiKey", maskKey(config.getAi().getOpenai().getApiKey()));
        configStatus.put("ai.openai.model", config.getAi().getOpenai().getModel());
        configStatus.put("authentication.spotify.clientId", maskKey(config.getAuthentication().getSpotify().getClientId()));
        configStatus.put("authentication.spotify.tokenUrl", config.getAuthentication().getSpotify().getTokenUrl());
        
        // Check if keys are set
        boolean openrouterKeySet = config.getAi().getOpenai().getApiKey() != null && 
                                   !config.getAi().getOpenai().getApiKey().isEmpty();
        boolean spotifyClientIdSet = config.getAuthentication().getSpotify().getClientId() != null && 
                                     !config.getAuthentication().getSpotify().getClientId().isEmpty();
        boolean spotifyClientSecretSet = config.getAuthentication().getSpotify().getClientSecret() != null && 
                                         !config.getAuthentication().getSpotify().getClientSecret().isEmpty();
        
        status.put("environmentVariables", envVars);
        status.put("configuration", configStatus);
        status.put("readyToValidate", openrouterKeySet && spotifyClientIdSet && spotifyClientSecretSet);
        status.put("missingKeys", getMissingKeys(openrouterKeySet, spotifyClientIdSet, spotifyClientSecretSet));
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Live-test each configured API key by hitting the provider's cheapest endpoint.
     * Returns { "openai": {...}, "openrouter": {...}, "spotify": {...} }
     */
    @GetMapping("/check-keys")
    public ResponseEntity<Map<String, Object>> checkKeys() {
        Map<String, Object> results = new LinkedHashMap<>();

        results.put("openai",     testOpenAI());
        results.put("openrouter", testOpenRouter());
        results.put("spotify",    testSpotify());

        boolean allOk = results.values().stream()
                .map(v -> ((Map<?, ?>) v).get("ok"))
                .allMatch(Boolean.TRUE::equals);
        results.put("allOk", allOk);

        return ResponseEntity.ok(results);
    }

    // ── OpenAI ──────────────────────────────────────────────────────────────

    private Map<String, Object> testOpenAI() {
        String key = config.getAi().getOpenai().getApiKey();
        if (isPlaceholder(key)) {
            return result(false, "Key not configured", maskKey(key));
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/models"))
                    .header("Authorization", "Bearer " + key)
                    .GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = res.statusCode() == 200;
            return result(ok, ok ? "Key valid" : "HTTP " + res.statusCode(), maskKey(key));
        } catch (Exception e) {
            log.warn("OpenAI key test failed: {}", e.getMessage());
            return result(false, e.getMessage(), maskKey(key));
        }
    }

    // ── OpenRouter ───────────────────────────────────────────────────────────

    private Map<String, Object> testOpenRouter() {
        // OpenRouter key is read from OPENROUTER_API_KEY env var
        String key = System.getenv("OPENROUTER_API_KEY");
        if (isPlaceholder(key)) {
            return result(false, "Key not configured", maskKey(key));
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/models"))
                    .header("Authorization", "Bearer " + key)
                    .GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = res.statusCode() == 200;
            return result(ok, ok ? "Key valid" : "HTTP " + res.statusCode(), maskKey(key));
        } catch (Exception e) {
            log.warn("OpenRouter key test failed: {}", e.getMessage());
            return result(false, e.getMessage(), maskKey(key));
        }
    }

    // ── Spotify ──────────────────────────────────────────────────────────────

    private Map<String, Object> testSpotify() {
        String clientId     = config.getAuthentication().getSpotify().getClientId();
        String clientSecret = config.getAuthentication().getSpotify().getClientSecret();
        if (isPlaceholder(clientId) || isPlaceholder(clientSecret)) {
            return result(false, "Client ID or Secret not configured",
                    maskKey(clientId) + " / " + maskKey(clientSecret));
        }
        try {
            String credentials = java.util.Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://accounts.spotify.com/api/token"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = res.statusCode() == 200;
            return result(ok,
                    ok ? "Credentials valid — token obtained" : "HTTP " + res.statusCode() + ": " + res.body(),
                    maskKey(clientId));
        } catch (Exception e) {
            log.warn("Spotify key test failed: {}", e.getMessage());
            return result(false, e.getMessage(), maskKey(clientId));
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> result(boolean ok, String message, String maskedKey) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", ok);
        m.put("message", message);
        m.put("key", maskedKey);
        return m;
    }

    private boolean isPlaceholder(String key) {
        return key == null || key.isBlank()
                || key.startsWith("your_") || key.equals("sk-placeholder");
    }

    /**
     * Simple test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Debug endpoint is working",
                "timestamp", System.currentTimeMillis()
        ));
    }
    
    private String maskKey(String key) {
        if (key == null || key.isEmpty()) {
            return "NOT_SET";
        }
        if (key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
    
    private java.util.List<String> getMissingKeys(boolean openrouter, boolean spotifyId, boolean spotifySecret) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (!openrouter) missing.add("OPENROUTER_API_KEY");
        if (!spotifyId) missing.add("SPOTIFY_CLIENT_ID");
        if (!spotifySecret) missing.add("SPOTIFY_CLIENT_SECRET");
        return missing;
    }
}

// Made with Bob
