package com.docvalidator.mcp;

import com.docvalidator.auth.SpotifyAuthManager;
import com.docvalidator.config.DocValidatorConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP Server 2: Live API Context Provider
 * 
 * Exposes runtime API behavior and responses as context for AI agents.
 * Provides tools to execute API calls, monitor responses, and track actual behavior.
 * 
 * MCP Tools Exposed:
 * - execute_api_call: Make live API requests
 * - get_response_schema: Extract actual response structure
 * - check_endpoint_availability: Verify endpoint accessibility
 * - validate_oauth_token: Check OAuth token validity
 * - get_api_metrics: Get performance metrics
 */
@Slf4j
@Component
public class LiveApiMcpServer {
    
    private final DocValidatorConfig config;
    private final SpotifyAuthManager authManager;
    private final ObjectMapper objectMapper;
    
    private final Map<String, ApiMetrics> metricsCache = new HashMap<>();
    
    public LiveApiMcpServer(DocValidatorConfig config, SpotifyAuthManager authManager) {
        this.config = config;
        this.authManager = authManager;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * MCP Tool: execute_api_call
     * Make a live API request and capture the response
     */
    public Map<String, Object> executeApiCall(String method, String path, 
                                              Map<String, String> headers,
                                              Map<String, String> queryParams,
                                              String body) {
        log.info("MCP Tool: execute_api_call called for {} {}", method, path);
        
        try {
            // Get OAuth token
            String token = authManager.getAccessToken();
            
            // Build request
            var request = RestAssured.given()
                    .baseUri(config.getTargetApi().getBaseUrl())
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json");
            
            // Add custom headers
            if (headers != null) {
                headers.forEach(request::header);
            }
            
            // Add query parameters
            if (queryParams != null) {
                queryParams.forEach(request::queryParam);
            }
            
            // Add body if present
            if (body != null && !body.isEmpty()) {
                request.body(body);
            }
            
            // Execute request and measure time
            long startTime = System.currentTimeMillis();
            Response response = executeRequest(request, method, path);
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Update metrics
            updateMetrics(path, response.getStatusCode(), responseTime);
            
            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("statusCode", response.getStatusCode());
            result.put("responseTime", responseTime);
            result.put("headers", response.getHeaders().asList().stream()
                    .collect(HashMap::new, 
                            (m, h) -> m.put(h.getName(), h.getValue()), 
                            HashMap::putAll));
            
            try {
                result.put("body", objectMapper.readTree(response.getBody().asString()));
            } catch (Exception e) {
                result.put("body", response.getBody().asString());
            }
            
            return result;
            
        } catch (Exception e) {
            log.debug("MCP API call failed: {}", e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * MCP Tool: get_response_schema
     * Extract the actual schema structure from a response
     */
    public Map<String, Object> getResponseSchema(String method, String path) {
        log.info("MCP Tool: get_response_schema called for {} {}", method, path);
        
        try {
            // Execute the API call
            Map<String, Object> callResult = executeApiCall(method, path, null, null, null);
            
            if (!Boolean.TRUE.equals(callResult.get("success"))) {
                return callResult;
            }
            
            // Extract schema from response body
            Object body = callResult.get("body");
            Map<String, Object> schema = extractSchema(body);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("path", path);
            result.put("method", method);
            result.put("schema", schema);
            result.put("statusCode", callResult.get("statusCode"));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting response schema", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * MCP Tool: check_endpoint_availability
     * Verify if an endpoint is accessible
     */
    public Map<String, Object> checkEndpointAvailability(String method, String path) {
        log.info("MCP Tool: check_endpoint_availability called for {} {}", method, path);
        
        try {
            Map<String, Object> callResult = executeApiCall(method, path, null, null, null);
            
            boolean available = Boolean.TRUE.equals(callResult.get("success")) && 
                               (Integer) callResult.get("statusCode") < 500;
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("available", available);
            result.put("statusCode", callResult.get("statusCode"));
            result.put("responseTime", callResult.get("responseTime"));
            result.put("checkedAt", LocalDateTime.now().toString());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error checking endpoint availability", e);
            return Map.of(
                "success", true,
                "available", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * MCP Tool: validate_oauth_token
     * Check if the OAuth token is valid
     */
    public Map<String, Object> validateOauthToken() {
        log.info("MCP Tool: validate_oauth_token called");
        
        try {
            String token = authManager.getAccessToken();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("valid", token != null && !token.isEmpty());
            result.put("tokenLength", token != null ? token.length() : 0);
            result.put("checkedAt", LocalDateTime.now().toString());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error validating OAuth token", e);
            return Map.of(
                "success", false,
                "valid", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * MCP Tool: get_api_metrics
     * Get performance metrics for API endpoints
     */
    public Map<String, Object> getApiMetrics(String path) {
        log.info("MCP Tool: get_api_metrics called for path: {}", path);
        
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            if (path != null) {
                ApiMetrics metrics = metricsCache.get(path);
                if (metrics != null) {
                    result.put("path", path);
                    result.put("metrics", metrics.toMap());
                } else {
                    result.put("error", "No metrics found for path: " + path);
                }
            } else {
                // Return all metrics
                Map<String, Map<String, Object>> allMetrics = new HashMap<>();
                metricsCache.forEach((p, m) -> allMetrics.put(p, m.toMap()));
                result.put("allMetrics", allMetrics);
                result.put("totalEndpoints", metricsCache.size());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting API metrics", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * Execute HTTP request based on method
     */
    private Response executeRequest(io.restassured.specification.RequestSpecification request, 
                                    String method, String path) {
        return switch (method.toUpperCase()) {
            case "GET" -> request.get(path);
            case "POST" -> request.post(path);
            case "PUT" -> request.put(path);
            case "DELETE" -> request.delete(path);
            case "PATCH" -> request.patch(path);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }
    
    /**
     * Extract schema structure from response body
     */
    private Map<String, Object> extractSchema(Object body) {
        Map<String, Object> schema = new HashMap<>();
        
        if (body instanceof JsonNode jsonNode) {
            schema.put("type", getJsonNodeType(jsonNode));
            
            if (jsonNode.isObject()) {
                Map<String, String> properties = new HashMap<>();
                jsonNode.fields().forEachRemaining(entry -> 
                    properties.put(entry.getKey(), getJsonNodeType(entry.getValue()))
                );
                schema.put("properties", properties);
            } else if (jsonNode.isArray() && jsonNode.size() > 0) {
                schema.put("items", extractSchema(jsonNode.get(0)));
            }
        } else {
            schema.put("type", body != null ? body.getClass().getSimpleName() : "null");
        }
        
        return schema;
    }
    
    /**
     * Get JSON node type as string
     */
    private String getJsonNodeType(JsonNode node) {
        if (node.isObject()) return "object";
        if (node.isArray()) return "array";
        if (node.isTextual()) return "string";
        if (node.isNumber()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isNull()) return "null";
        return "unknown";
    }
    
    /**
     * Update metrics for an endpoint
     */
    private void updateMetrics(String path, int statusCode, long responseTime) {
        ApiMetrics metrics = metricsCache.computeIfAbsent(path, k -> new ApiMetrics());
        metrics.addCall(statusCode, responseTime);
    }
    
    /**
     * Clear metrics cache
     */
    public void clearMetrics() {
        log.info("Clearing API metrics cache");
        metricsCache.clear();
    }
    
    /**
     * API Metrics tracking class
     */
    private static class ApiMetrics {
        private int totalCalls = 0;
        private int successfulCalls = 0;
        private int failedCalls = 0;
        private long totalResponseTime = 0;
        private long minResponseTime = Long.MAX_VALUE;
        private long maxResponseTime = 0;
        
        public void addCall(int statusCode, long responseTime) {
            totalCalls++;
            if (statusCode >= 200 && statusCode < 300) {
                successfulCalls++;
            } else {
                failedCalls++;
            }
            
            totalResponseTime += responseTime;
            minResponseTime = Math.min(minResponseTime, responseTime);
            maxResponseTime = Math.max(maxResponseTime, responseTime);
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalCalls", totalCalls);
            map.put("successfulCalls", successfulCalls);
            map.put("failedCalls", failedCalls);
            map.put("successRate", totalCalls > 0 ? (double) successfulCalls / totalCalls * 100 : 0);
            map.put("averageResponseTime", totalCalls > 0 ? totalResponseTime / totalCalls : 0);
            map.put("minResponseTime", minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime);
            map.put("maxResponseTime", maxResponseTime);
            return map;
        }
    }
}

// Made with Bob
