package com.docvalidator.agent;

import com.docvalidator.config.DocValidatorConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal chat-completions client for OpenAI-compatible providers.
 */
@Slf4j
@Component
public class AiChatClient {

    private static final MediaType JSON = MediaType.get("application/json");
    private static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final DocValidatorConfig config;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public AiChatClient(DocValidatorConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(90))
                .writeTimeout(Duration.ofSeconds(30))
                .build();

        if (isConfigured()) {
            log.info("AI chat client configured for provider={} model={}", getProvider(), getModel());
        } else {
            log.info("AI chat client disabled (provider={}, key configured=false)", getProvider());
        }
    }

    public boolean isConfigured() {
        return !isPlaceholder(getApiKey()) && !isPlaceholder(getModel());
    }

    public String complete(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, null);
    }

    public String complete(String systemPrompt, String userPrompt, Integer maxTokensOverride) {
        if (!isConfigured()) {
            log.debug("AI chat client not configured, skipping completion");
            return "[]";
        }

        try {
            Map<String, Object> payload = Map.of(
                    "model", getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", getTemperature(),
                    "max_tokens", maxTokensOverride != null ? maxTokensOverride : getMaxTokens()
            );

            Request.Builder requestBuilder = new Request.Builder()
                    .url(getChatCompletionsUrl())
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload), JSON));

            if ("openrouter".equalsIgnoreCase(getProvider())) {
                addOptionalHeader(requestBuilder, "HTTP-Referer", config.getAi().getOpenrouter().getSiteUrl());
                addOptionalHeader(requestBuilder, "X-Title", config.getAi().getOpenrouter().getAppName());
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("AI completion failed: provider={} status={} body={}",
                            getProvider(), response.code(), body);
                    return "[]";
                }

                JsonNode root = objectMapper.readTree(body);
                JsonNode content = root.path("choices").path(0).path("message").path("content");
                return content.isMissingNode() || content.isNull() ? "[]" : content.asText();
            }
        } catch (Exception e) {
            log.error("Error calling AI provider {}", getProvider(), e);
            return "[]";
        }
    }

    private void addOptionalHeader(Request.Builder requestBuilder, String name, String value) {
        if (!isPlaceholder(value)) {
            requestBuilder.header(name, value);
        }
    }

    private String getProvider() {
        return config.getAi().getProvider();
    }

    private String getApiKey() {
        if ("openrouter".equalsIgnoreCase(getProvider())) {
            return config.getAi().getOpenrouter().getApiKey();
        }
        return config.getAi().getOpenai().getApiKey();
    }

    private String getModel() {
        if ("openrouter".equalsIgnoreCase(getProvider())) {
            return config.getAi().getOpenrouter().getModel();
        }
        return config.getAi().getOpenai().getModel();
    }

    private double getTemperature() {
        Double temperature = "openrouter".equalsIgnoreCase(getProvider())
                ? config.getAi().getOpenrouter().getTemperature()
                : config.getAi().getOpenai().getTemperature();
        return temperature != null ? temperature : 0.2;
    }

    private int getMaxTokens() {
        Integer maxTokens = "openrouter".equalsIgnoreCase(getProvider())
                ? config.getAi().getOpenrouter().getMaxTokens()
                : config.getAi().getOpenai().getMaxTokens();
        return maxTokens != null ? maxTokens : 2000;
    }

    private String getChatCompletionsUrl() {
        if ("openrouter".equalsIgnoreCase(getProvider())) {
            String baseUrl = config.getAi().getOpenrouter().getBaseUrl();
            String normalizedBaseUrl = isPlaceholder(baseUrl)
                    ? "https://openrouter.ai/api/v1"
                    : baseUrl.replaceAll("/+$", "");
            return normalizedBaseUrl + "/chat/completions";
        }
        return OPENAI_CHAT_COMPLETIONS_URL;
    }

    private boolean isPlaceholder(String value) {
        return value == null || value.isBlank()
                || value.startsWith("your_")
                || value.equals("sk-placeholder");
    }
}
