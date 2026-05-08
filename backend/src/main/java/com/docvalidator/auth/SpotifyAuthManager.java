package com.docvalidator.auth;

import com.docvalidator.config.DocValidatorConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Manages OAuth 2.0 authentication for Spotify API.
 * Handles token acquisition, refresh, and caching.
 */
@Slf4j
@Component
public class SpotifyAuthManager {
    
    private final DocValidatorConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private String accessToken;
    private LocalDateTime tokenExpiresAt;
    
    public SpotifyAuthManager(DocValidatorConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get valid access token (cached or new)
     */
    public String getAccessToken() {
        if (isTokenValid()) {
            log.debug("Using cached access token");
            return accessToken;
        }
        
        log.info("Acquiring new access token");
        return acquireNewToken();
    }
    
    /**
     * Check if current token is valid
     */
    private boolean isTokenValid() {
        if (accessToken == null || tokenExpiresAt == null) {
            return false;
        }
        
        // Consider token invalid 5 minutes before actual expiry
        LocalDateTime bufferTime = LocalDateTime.now().plusMinutes(5);
        return tokenExpiresAt.isAfter(bufferTime);
    }
    
    /**
     * Acquire new access token using Client Credentials flow
     */
    private String acquireNewToken() {
        try {
            String clientId = config.getAuthentication().getSpotify().getClientId();
            String clientSecret = config.getAuthentication().getSpotify().getClientSecret();
            String tokenUrl = config.getAuthentication().getSpotify().getTokenUrl();
            
            // Create Basic Auth header
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            
            // Build request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Authorization", "Basic " + encodedCredentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();
            
            // Execute request
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new AuthenticationException(
                        "Failed to acquire token: " + response.statusCode() + " - " + response.body());
            }
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response.body());
            accessToken = jsonResponse.get("access_token").asText();
            int expiresIn = jsonResponse.get("expires_in").asInt();
            
            // Calculate expiry time
            tokenExpiresAt = LocalDateTime.now().plusSeconds(expiresIn);
            
            log.info("Access token acquired successfully, expires at: {}", tokenExpiresAt);
            
            return accessToken;
            
        } catch (IOException | InterruptedException e) {
            log.error("Error acquiring access token", e);
            throw new AuthenticationException("Failed to acquire access token", e);
        }
    }
    
    /**
     * Clear cached token (force refresh on next request)
     */
    public void clearToken() {
        log.info("Clearing cached access token");
        accessToken = null;
        tokenExpiresAt = null;
    }
    
    /**
     * Get authorization header value
     */
    public String getAuthorizationHeader() {
        return "Bearer " + getAccessToken();
    }
    
    /**
     * Exception for authentication errors
     */
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
        
        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// Made with Bob
