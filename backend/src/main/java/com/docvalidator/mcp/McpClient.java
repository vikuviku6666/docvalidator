package com.docvalidator.mcp;

import com.docvalidator.model.ApiEndpoint;
import com.docvalidator.parser.OpenApiParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unified MCP client for AI agents to access both documentation and live API context.
 * Provides caching, error handling, and a clean interface for MCP operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpClient {
    
    private final DocumentationMcpServer documentationMcp;
    private final LiveApiMcpServer liveApiMcp;
    private final McpCache cache;
    
    // Cache key prefixes
    private static final String CACHE_OPENAPI_SPEC = "mcp:doc:spec";
    private static final String CACHE_ENDPOINT_PREFIX = "mcp:doc:endpoint:";
    private static final String CACHE_SCHEMA_PREFIX = "mcp:doc:schema:";
    private static final String CACHE_ENDPOINTS_LIST = "mcp:doc:endpoints:list";
    private static final String CACHE_AUTH_PREFIX = "mcp:doc:auth:";
    private static final String CACHE_RESPONSE_SCHEMA_PREFIX = "mcp:live:schema:";
    private static final String CACHE_AVAILABILITY_PREFIX = "mcp:live:availability:";
    private static final String CACHE_METRICS = "mcp:live:metrics";
    
    // Cache TTLs
    private static final Duration SPEC_TTL = Duration.ofMinutes(30);
    private static final Duration ENDPOINT_TTL = Duration.ofMinutes(15);
    private static final Duration SCHEMA_TTL = Duration.ofMinutes(15);
    private static final Duration AUTH_TTL = Duration.ofMinutes(15);
    private static final Duration LIVE_TTL = Duration.ofMinutes(1);
    
    // ==================== Documentation MCP Methods ====================
    
    /**
     * Get the complete OpenAPI specification.
     * Cached for 30 minutes.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOpenApiSpec(String specUrl) {
        String cacheKey = CACHE_OPENAPI_SPEC + ":" + specUrl;
        Map<String, Object> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }
        
        try {
            Map<String, Object> result = documentationMcp.getOpenApiSpec(specUrl);
            if (isSuccessful(result)) {
                cache.put(cacheKey, result, SPEC_TTL);
            }
            return result;
        } catch (Exception e) {
            log.error("Error fetching OpenAPI spec", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Get detailed information about a specific endpoint.
     * Cached for 15 minutes.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEndpointDetails(String path, String method) {
        String cacheKey = CACHE_ENDPOINT_PREFIX + method + ":" + path;
        
        Map<String, Object> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }
        
        try {
            Map<String, Object> result = documentationMcp.getEndpointDetails(path, method);
            if (isSuccessful(result)) {
                cache.put(cacheKey, result, ENDPOINT_TTL);
            }
            return result;
        } catch (Exception e) {
            log.error("Error fetching endpoint details for {} {}", method, path, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Get a specific schema definition.
     * Cached for 15 minutes.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSchemaDefinition(String schemaName) {
        String cacheKey = CACHE_SCHEMA_PREFIX + schemaName;
        
        Map<String, Object> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }
        
        try {
            Map<String, Object> result = documentationMcp.getSchemaDefinition(schemaName);
            if (isSuccessful(result)) {
                cache.put(cacheKey, result, SCHEMA_TTL);
            }
            return result;
        } catch (Exception e) {
            log.error("Error fetching schema definition for {}", schemaName, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * List all endpoints, optionally filtered by tag and deprecated status.
     * Cached for 15 minutes.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listEndpoints(String tag, Boolean deprecated) {
        String cacheKey = CACHE_ENDPOINTS_LIST +
            (tag != null ? ":" + tag : "") +
            (deprecated != null ? ":deprecated=" + deprecated : "");
        
        Map<String, Object> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }
        
        try {
            Map<String, Object> result = documentationMcp.listEndpoints(tag, deprecated);
            if (isSuccessful(result)) {
                cache.put(cacheKey, result, ENDPOINT_TTL);
            }
            return result;
        } catch (Exception e) {
            log.error("Error listing endpoints with tag {} and deprecated {}", tag, deprecated, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Get authentication requirements for the API.
     * Cached for 15 minutes.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAuthRequirements() {
        Map<String, Object> cached = cache.get(CACHE_AUTH_PREFIX, Map.class);
        if (cached != null) {
            return cached;
        }
        
        try {
            Map<String, Object> result = documentationMcp.getAuthRequirements();
            if (isSuccessful(result)) {
                cache.put(CACHE_AUTH_PREFIX, result, AUTH_TTL);
            }
            return result;
        } catch (Exception e) {
            log.error("Error fetching auth requirements", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    // ==================== Live API MCP Methods ====================
    
    /**
     * Execute a live API call.
     * Not cached as it represents real-time state.
     */
    public Map<String, Object> executeApiCall(
        String method,
        String path,
        Map<String, String> headers,
        Map<String, String> queryParams,
        String body
    ) {
        try {
            return liveApiMcp.executeApiCall(method, path, headers, queryParams, body);
        } catch (Exception e) {
            log.debug("MCP API call to {} {} failed: {}", method, path, e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Get the actual response schema from a live API call.
     * Cached for 1 minute (short TTL as API may change).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResponseSchema(String path, String method) {
        String cacheKey = CACHE_RESPONSE_SCHEMA_PREFIX + method + ":" + path;
        
        Map<String, Object> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }
        
        try {
            Map<String, Object> result = liveApiMcp.getResponseSchema(path, method);
            if (isSuccessful(result)) {
                cache.put(cacheKey, result, LIVE_TTL);
            }
            return result;
        } catch (Exception e) {
            log.error("Error fetching response schema for {} {}", method, path, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Check if an endpoint is available.
     * Cached for 1 minute.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkEndpointAvailability(String method, String path) {
        String cacheKey = CACHE_AVAILABILITY_PREFIX + method + ":" + path;
        
        Map<String, Object> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }
        
        try {
            Map<String, Object> result = liveApiMcp.checkEndpointAvailability(method, path);
            if (isSuccessful(result)) {
                cache.put(cacheKey, result, LIVE_TTL);
            }
            return result;
        } catch (Exception e) {
            log.error("Error checking availability for {} {}", method, path, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Validate the current OAuth token.
     * Not cached as tokens may be revoked.
     */
    public Map<String, Object> validateOAuthToken() {
        try {
            return liveApiMcp.validateOauthToken();
        } catch (Exception e) {
            log.error("Error validating OAuth token", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Get API performance metrics.
     * Cached for 1 minute.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getApiMetrics(String path) {
        String cacheKey = CACHE_METRICS + (path != null ? ":" + path : "");
        
        Map<String, Object> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }
        
        try {
            Map<String, Object> result = liveApiMcp.getApiMetrics(path);
            if (isSuccessful(result)) {
                cache.put(cacheKey, result, LIVE_TTL);
            }
            return result;
        } catch (Exception e) {
            log.error("Error fetching API metrics for path {}", path, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Get endpoint details as ApiEndpoint object.
     * Convenience method for AI agents.
     */
    @SuppressWarnings("unchecked")
    public ApiEndpoint getEndpoint(String path, String method) {
        Map<String, Object> result = getEndpointDetails(path, method);
        
        if (!isSuccessful(result)) {
            return null;
        }
        
        Map<String, Object> endpointData = (Map<String, Object>) result.get("endpoint");
        if (endpointData == null) {
            return null;
        }
        
        // Convert map to ApiEndpoint
        // This is a simplified conversion - in production, use proper mapping
        return convertToApiEndpoint(endpointData);
    }
    
    /**
     * Get all endpoints as ApiEndpoint objects.
     */
    @SuppressWarnings("unchecked")
    public List<ApiEndpoint> getAllEndpoints() {
        Map<String, Object> result = listEndpoints(null, null);
        
        if (!isSuccessful(result)) {
            return List.of();
        }
        
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) result.get("endpoints");
        if (endpoints == null) {
            return List.of();
        }
        
        return endpoints.stream()
            .map(this::convertToApiEndpoint)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if an MCP response was successful.
     */
    private boolean isSuccessful(Map<String, Object> result) {
        return result != null && Boolean.TRUE.equals(result.get("success"));
    }
    
    /**
     * Convert map to ApiEndpoint object.
     * Simplified conversion - extend as needed.
     */
    private ApiEndpoint convertToApiEndpoint(Map<String, Object> data) {
        // This is a placeholder - implement proper conversion based on your needs
        // For now, return null and let the calling code handle the map directly
        return null;
    }
    
    // ==================== Cache Management ====================
    
    /**
     * Invalidate all documentation cache.
     * Call this when OpenAPI spec is updated.
     */
    public void invalidateDocumentationCache() {
        cache.invalidatePattern("mcp:doc:.*");
        log.info("Invalidated all documentation cache");
    }
    
    /**
     * Invalidate all live API cache.
     * Call this when API behavior changes.
     */
    public void invalidateLiveApiCache() {
        cache.invalidatePattern("mcp:live:.*");
        log.info("Invalidated all live API cache");
    }
    
    /**
     * Invalidate cache for a specific endpoint.
     */
    public void invalidateEndpointCache(String path, String method) {
        String pattern = ".*:" + method + ":" + path.replace("/", "\\/");
        cache.invalidatePattern(pattern);
        log.info("Invalidated cache for endpoint {} {}", method, path);
    }
    
    /**
     * Clear all MCP cache.
     */
    public void clearCache() {
        cache.clear();
        log.info("Cleared all MCP cache");
    }
    
    /**
     * Get cache statistics.
     */
    public McpCache.CacheStats getCacheStats() {
        return cache.getStats();
    }
    
    /**
     * Perform cache cleanup (remove expired entries).
     */
    public void cleanupCache() {
        cache.cleanup();
    }
}

// Made with Bob
