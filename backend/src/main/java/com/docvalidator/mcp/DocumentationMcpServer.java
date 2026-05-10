package com.docvalidator.mcp;

import com.docvalidator.config.DocValidatorConfig;
import com.docvalidator.model.ApiEndpoint;
import com.docvalidator.parser.OpenApiParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Server 1: Documentation Context Provider
 * 
 * Exposes structured API documentation as context for AI agents.
 * Provides tools to access OpenAPI specifications, endpoint details, and schemas.
 * 
 * MCP Tools Exposed:
 * - get_openapi_spec: Retrieve full OpenAPI specification
 * - get_endpoint_details: Get specific endpoint documentation
 * - get_schema_definition: Retrieve schema definitions
 * - list_endpoints: List all documented endpoints
 * - get_auth_requirements: Get authentication requirements
 */
@Slf4j
@Component
public class DocumentationMcpServer {
    
    private final DocValidatorConfig config;
    private final OpenApiParser parser;
    private final ObjectMapper objectMapper;
    
    private OpenAPI cachedOpenApiSpec;
    private List<ApiEndpoint> cachedEndpoints;
    
    public DocumentationMcpServer(DocValidatorConfig config, OpenApiParser parser) {
        this.config = config;
        this.parser = parser;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * MCP Tool: get_openapi_spec
     * Retrieve the full OpenAPI specification
     */
    public Map<String, Object> getOpenApiSpec(String specUrl) {
        log.info("MCP Tool: get_openapi_spec called with URL: {}", specUrl);
        
        try {
            if (cachedOpenApiSpec == null || !specUrl.equals(config.getTargetApi().getOpenapiSpecUrl())) {
                cachedOpenApiSpec = new OpenAPIV3Parser().read(specUrl);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("spec", cachedOpenApiSpec);
            result.put("info", Map.of(
                "title", cachedOpenApiSpec.getInfo().getTitle(),
                "version", cachedOpenApiSpec.getInfo().getVersion(),
                "description", cachedOpenApiSpec.getInfo().getDescription()
            ));
            result.put("servers", cachedOpenApiSpec.getServers());
            result.put("pathCount", cachedOpenApiSpec.getPaths().size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error retrieving OpenAPI spec", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * MCP Tool: get_endpoint_details
     * Get detailed information about a specific endpoint
     */
    public Map<String, Object> getEndpointDetails(String path, String method) {
        log.info("MCP Tool: get_endpoint_details called for {} {}", method, path);
        
        try {
            if (cachedEndpoints == null) {
                OpenApiParser.ParseResult parseResult = parser.parseFromUrl(config.getTargetApi().getOpenapiSpecUrl());
                cachedEndpoints = parseResult.endpoints();
            }
            
            ApiEndpoint endpoint = cachedEndpoints.stream()
                .filter(e -> e.getPath().equals(path) && e.getMethod().name().equalsIgnoreCase(method))
                .findFirst()
                .orElse(null);
            
            if (endpoint == null) {
                return Map.of(
                    "success", false,
                    "error", "Endpoint not found: " + method + " " + path
                );
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> endpointData = new HashMap<>();
            endpointData.put("path", endpoint.getPath());
            endpointData.put("method", endpoint.getMethod());
            endpointData.put("summary", endpoint.getSummary());
            endpointData.put("description", endpoint.getDescription());
            endpointData.put("operationId", endpoint.getOperationId());
            endpointData.put("tags", endpoint.getTags());
            endpointData.put("parameters", endpoint.getParameters());
            endpointData.put("requestBody", endpoint.getRequestBody());
            endpointData.put("responses", endpoint.getResponses());
            endpointData.put("requiredScopes", endpoint.getRequiredScopes());
            endpointData.put("deprecated", endpoint.isDeprecated());
            
            result.put("endpoint", endpointData);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting endpoint details", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * MCP Tool: get_schema_definition
     * Retrieve schema definitions from the OpenAPI spec
     */
    public Map<String, Object> getSchemaDefinition(String schemaName) {
        log.info("MCP Tool: get_schema_definition called for schema: {}", schemaName);
        
        try {
            if (cachedOpenApiSpec == null) {
                cachedOpenApiSpec = new OpenAPIV3Parser().read(config.getTargetApi().getOpenapiSpecUrl());
            }
            
            if (cachedOpenApiSpec.getComponents() == null || 
                cachedOpenApiSpec.getComponents().getSchemas() == null) {
                return Map.of(
                    "success", false,
                    "error", "No schemas found in OpenAPI spec"
                );
            }
            
            var schema = cachedOpenApiSpec.getComponents().getSchemas().get(schemaName);
            
            if (schema == null) {
                return Map.of(
                    "success", false,
                    "error", "Schema not found: " + schemaName
                );
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("schemaName", schemaName);
            result.put("schema", Map.of(
                "type", schema.getType(),
                "properties", schema.getProperties() != null ? schema.getProperties() : Map.of(),
                "required", schema.getRequired() != null ? schema.getRequired() : List.of(),
                "description", schema.getDescription() != null ? schema.getDescription() : ""
            ));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting schema definition", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * MCP Tool: list_endpoints
     * List all documented endpoints
     */
    public Map<String, Object> listEndpoints(String tag, Boolean deprecated) {
        log.info("MCP Tool: list_endpoints called with tag: {}, deprecated: {}", tag, deprecated);
        
        try {
            if (cachedEndpoints == null) {
                OpenApiParser.ParseResult parseResult = parser.parseFromUrl(config.getTargetApi().getOpenapiSpecUrl());
                cachedEndpoints = parseResult.endpoints();
            }
            
            List<ApiEndpoint> filteredEndpoints = cachedEndpoints.stream()
                .filter(e -> tag == null || e.getTags().contains(tag))
                .filter(e -> deprecated == null || e.isDeprecated() == deprecated)
                .collect(Collectors.toList());
            
            List<Map<String, Object>> endpointList = filteredEndpoints.stream()
                .map(e -> Map.of(
                    "path", (Object) e.getPath(),
                    "method", e.getMethod().name(),
                    "summary", e.getSummary() != null ? e.getSummary() : "",
                    "operationId", e.getOperationId() != null ? e.getOperationId() : "",
                    "tags", e.getTags(),
                    "deprecated", e.isDeprecated()
                ))
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalCount", filteredEndpoints.size());
            result.put("endpoints", endpointList);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error listing endpoints", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * MCP Tool: get_auth_requirements
     * Get authentication requirements for the API
     */
    public Map<String, Object> getAuthRequirements() {
        log.info("MCP Tool: get_auth_requirements called");
        
        try {
            if (cachedOpenApiSpec == null) {
                try {
                    cachedOpenApiSpec = new OpenAPIV3Parser().read(config.getTargetApi().getOpenapiSpecUrl());
                    if (cachedOpenApiSpec == null) {
                        log.debug("OpenAPI spec URL returned HTML instead of YAML/JSON. MCP auth requirements will be skipped. Validation will continue normally.");
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", false);
                        result.put("error", "OpenAPI spec URL returned invalid content (HTML instead of YAML/JSON)");
                        result.put("securitySchemes", Map.of());
                        return result;
                    }
                } catch (Exception e) {
                    log.debug("Could not parse OpenAPI spec from URL. MCP auth requirements will be skipped. Validation will continue normally.");
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("error", "OpenAPI spec parsing failed - validation will continue without MCP context");
                    result.put("securitySchemes", Map.of());
                    return result;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            if (cachedOpenApiSpec != null && cachedOpenApiSpec.getComponents() != null &&
                cachedOpenApiSpec.getComponents().getSecuritySchemes() != null) {
                result.put("securitySchemes", cachedOpenApiSpec.getComponents().getSecuritySchemes());
            } else {
                result.put("securitySchemes", Map.of());
            }
            
            if (cachedOpenApiSpec.getSecurity() != null) {
                result.put("globalSecurity", cachedOpenApiSpec.getSecurity());
            } else {
                result.put("globalSecurity", List.of());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting auth requirements", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * Clear cached data
     */
    public void clearCache() {
        log.info("Clearing MCP server cache");
        cachedOpenApiSpec = null;
        cachedEndpoints = null;
    }
}

// Made with Bob
