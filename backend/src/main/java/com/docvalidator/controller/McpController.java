package com.docvalidator.controller;

import com.docvalidator.mcp.DocumentationMcpServer;
import com.docvalidator.mcp.LiveApiMcpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for MCP (Model Context Protocol) servers.
 * Exposes MCP tools as REST API endpoints for AI agents and external tools.
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = "*")
public class McpController {
    
    private final DocumentationMcpServer documentationServer;
    private final LiveApiMcpServer liveApiServer;
    
    public McpController(DocumentationMcpServer documentationServer, 
                        LiveApiMcpServer liveApiServer) {
        this.documentationServer = documentationServer;
        this.liveApiServer = liveApiServer;
    }
    
    // ==================== Documentation MCP Server Tools ====================
    
    /**
     * Get full OpenAPI specification
     */
    @GetMapping("/documentation/spec")
    public ResponseEntity<Map<String, Object>> getOpenApiSpec(
            @RequestParam String specUrl) {
        log.info("MCP API: Getting OpenAPI spec from {}", specUrl);
        Map<String, Object> result = documentationServer.getOpenApiSpec(specUrl);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get endpoint details
     */
    @GetMapping("/documentation/endpoint")
    public ResponseEntity<Map<String, Object>> getEndpointDetails(
            @RequestParam String path,
            @RequestParam String method) {
        log.info("MCP API: Getting endpoint details for {} {}", method, path);
        Map<String, Object> result = documentationServer.getEndpointDetails(path, method);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get schema definition
     */
    @GetMapping("/documentation/schema/{schemaName}")
    public ResponseEntity<Map<String, Object>> getSchemaDefinition(
            @PathVariable String schemaName) {
        log.info("MCP API: Getting schema definition for {}", schemaName);
        Map<String, Object> result = documentationServer.getSchemaDefinition(schemaName);
        return ResponseEntity.ok(result);
    }
    
    /**
     * List all endpoints
     */
    @GetMapping("/documentation/endpoints")
    public ResponseEntity<Map<String, Object>> listEndpoints(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean deprecated) {
        log.info("MCP API: Listing endpoints with tag={}, deprecated={}", tag, deprecated);
        Map<String, Object> result = documentationServer.listEndpoints(tag, deprecated);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get authentication requirements
     */
    @GetMapping("/documentation/auth")
    public ResponseEntity<Map<String, Object>> getAuthRequirements() {
        log.info("MCP API: Getting auth requirements");
        Map<String, Object> result = documentationServer.getAuthRequirements();
        return ResponseEntity.ok(result);
    }
    
    /**
     * Clear documentation cache
     */
    @PostMapping("/documentation/clear-cache")
    public ResponseEntity<Map<String, Object>> clearDocumentationCache() {
        log.info("MCP API: Clearing documentation cache");
        documentationServer.clearCache();
        return ResponseEntity.ok(Map.of("success", true, "message", "Cache cleared"));
    }
    
    // ==================== Live API MCP Server Tools ====================
    
    /**
     * Execute API call
     */
    @PostMapping("/live/execute")
    public ResponseEntity<Map<String, Object>> executeApiCall(
            @RequestBody ApiCallRequest request) {
        log.info("MCP API: Executing {} {}", request.method(), request.path());
        Map<String, Object> result = liveApiServer.executeApiCall(
            request.method(),
            request.path(),
            request.headers(),
            request.queryParams(),
            request.body()
        );
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get response schema
     */
    @GetMapping("/live/schema")
    public ResponseEntity<Map<String, Object>> getResponseSchema(
            @RequestParam String method,
            @RequestParam String path) {
        log.info("MCP API: Getting response schema for {} {}", method, path);
        Map<String, Object> result = liveApiServer.getResponseSchema(method, path);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Check endpoint availability
     */
    @GetMapping("/live/availability")
    public ResponseEntity<Map<String, Object>> checkEndpointAvailability(
            @RequestParam String method,
            @RequestParam String path) {
        log.info("MCP API: Checking availability of {} {}", method, path);
        Map<String, Object> result = liveApiServer.checkEndpointAvailability(method, path);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Validate OAuth token
     */
    @GetMapping("/live/validate-token")
    public ResponseEntity<Map<String, Object>> validateOauthToken() {
        log.info("MCP API: Validating OAuth token");
        Map<String, Object> result = liveApiServer.validateOauthToken();
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get API metrics
     */
    @GetMapping("/live/metrics")
    public ResponseEntity<Map<String, Object>> getApiMetrics(
            @RequestParam(required = false) String path) {
        log.info("MCP API: Getting metrics for path={}", path);
        Map<String, Object> result = liveApiServer.getApiMetrics(path);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Clear API metrics
     */
    @PostMapping("/live/clear-metrics")
    public ResponseEntity<Map<String, Object>> clearApiMetrics() {
        log.info("MCP API: Clearing API metrics");
        liveApiServer.clearMetrics();
        return ResponseEntity.ok(Map.of("success", true, "message", "Metrics cleared"));
    }
    
    // ==================== MCP Server Info ====================
    
    /**
     * Get MCP servers information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getMcpInfo() {
        return ResponseEntity.ok(Map.of(
            "servers", Map.of(
                "documentation", Map.of(
                    "name", "Documentation Context Provider",
                    "description", "Provides OpenAPI specification and endpoint documentation",
                    "tools", new String[]{
                        "get_openapi_spec",
                        "get_endpoint_details",
                        "get_schema_definition",
                        "list_endpoints",
                        "get_auth_requirements"
                    }
                ),
                "liveApi", Map.of(
                    "name", "Live API Context Provider",
                    "description", "Provides runtime API behavior and responses",
                    "tools", new String[]{
                        "execute_api_call",
                        "get_response_schema",
                        "check_endpoint_availability",
                        "validate_oauth_token",
                        "get_api_metrics"
                    }
                )
            ),
            "version", "1.0.0",
            "protocol", "MCP (Model Context Protocol)"
        ));
    }
    
    /**
     * API call request DTO
     */
    public record ApiCallRequest(
            String method,
            String path,
            Map<String, String> headers,
            Map<String, String> queryParams,
            String body
    ) {}
}

// Made with Bob
