package com.docvalidator.parser;

import com.docvalidator.model.ApiEndpoint;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses OpenAPI/Swagger specifications and extracts API endpoint information.
 */
@Slf4j
@Component
public class OpenApiParser {
    
    /**
     * Parse OpenAPI specification from URL
     */
    public ParseResult parseFromUrl(String specUrl) {
        log.info("Parsing OpenAPI specification from: {}", specUrl);
        
        try {
            SwaggerParseResult result = new OpenAPIV3Parser().readLocation(specUrl, null, null);
            
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                log.warn("Parser warnings: {}", result.getMessages());
            }
            
            OpenAPI openAPI = result.getOpenAPI();
            if (openAPI == null) {
                return ParseResult.failure("Failed to parse OpenAPI specification", result.getMessages());
            }
            
            List<ApiEndpoint> endpoints = extractEndpoints(openAPI);
            
            return ParseResult.success(openAPI, endpoints);
            
        } catch (Exception e) {
            log.error("Error parsing OpenAPI specification", e);
            return ParseResult.failure("Error parsing specification: " + e.getMessage(), 
                    Collections.singletonList(e.getMessage()));
        }
    }
    
    /**
     * Extract all endpoints from OpenAPI specification
     */
    private List<ApiEndpoint> extractEndpoints(OpenAPI openAPI) {
        List<ApiEndpoint> endpoints = new ArrayList<>();
        
        if (openAPI.getPaths() == null) {
            return endpoints;
        }
        
        openAPI.getPaths().forEach((path, pathItem) -> {
            endpoints.addAll(extractEndpointsFromPath(path, pathItem, openAPI));
        });
        
        log.info("Extracted {} endpoints from OpenAPI specification", endpoints.size());
        return endpoints;
    }
    
    /**
     * Extract endpoints from a single path
     */
    private List<ApiEndpoint> extractEndpointsFromPath(String path, PathItem pathItem, OpenAPI openAPI) {
        List<ApiEndpoint> endpoints = new ArrayList<>();
        
        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
        
        operations.forEach((httpMethod, operation) -> {
            try {
                ApiEndpoint endpoint = buildEndpoint(path, httpMethod, operation, pathItem, openAPI);
                endpoints.add(endpoint);
            } catch (Exception e) {
                log.error("Error extracting endpoint {} {}", httpMethod, path, e);
            }
        });
        
        return endpoints;
    }
    
    /**
     * Build ApiEndpoint from OpenAPI Operation
     */
    private ApiEndpoint buildEndpoint(String path, PathItem.HttpMethod httpMethod, 
                                     Operation operation, PathItem pathItem, OpenAPI openAPI) {
        
        ApiEndpoint.ApiEndpointBuilder builder = ApiEndpoint.builder()
                .id(UUID.randomUUID().toString())
                .path(path)
                .method(convertHttpMethod(httpMethod))
                .operationId(operation.getOperationId())
                .summary(operation.getSummary())
                .description(operation.getDescription())
                .deprecated(operation.getDeprecated() != null && operation.getDeprecated());
        
        // Extract parameters
        List<ApiEndpoint.Parameter> parameters = extractParameters(operation, pathItem);
        builder.parameters(parameters);
        
        // Extract request body
        if (operation.getRequestBody() != null) {
            ApiEndpoint.RequestBody requestBody = extractRequestBody(operation.getRequestBody());
            builder.requestBody(requestBody);
        }
        
        // Extract responses
        Map<Integer, ApiEndpoint.Response> responses = extractResponses(operation.getResponses());
        builder.responses(responses);
        
        // Extract tags
        if (operation.getTags() != null) {
            builder.tags(operation.getTags());
        }
        
        // Extract security requirements (OAuth scopes)
        List<String> requiredScopes = extractRequiredScopes(operation);
        builder.requiredScopes(requiredScopes);
        
        return builder.build();
    }
    
    /**
     * Extract parameters from operation
     */
    private List<ApiEndpoint.Parameter> extractParameters(Operation operation, PathItem pathItem) {
        List<ApiEndpoint.Parameter> parameters = new ArrayList<>();
        
        // Operation-level parameters
        if (operation.getParameters() != null) {
            operation.getParameters().forEach(param -> {
                parameters.add(convertParameter(param));
            });
        }
        
        // Path-level parameters
        if (pathItem.getParameters() != null) {
            pathItem.getParameters().forEach(param -> {
                parameters.add(convertParameter(param));
            });
        }
        
        return parameters;
    }
    
    /**
     * Convert OpenAPI Parameter to our model
     */
    private ApiEndpoint.Parameter convertParameter(Parameter param) {
        Schema<?> schema = param.getSchema();

        return ApiEndpoint.Parameter.builder()
                .name(param.getName())
                .in(convertParameterLocation(param.getIn()))
                .description(param.getDescription())
                .required(param.getRequired() != null && param.getRequired())
                .type(schema != null && schema.getType() != null ? schema.getType() : "string")
                .defaultValue(schema != null ? schema.getDefault() : null)
                .example(param.getExample() != null ? param.getExample() : (schema != null ? schema.getExample() : null))
                .build();
    }
    
    /**
     * Extract request body
     */
    private ApiEndpoint.RequestBody extractRequestBody(io.swagger.v3.oas.models.parameters.RequestBody requestBody) {
        Map<String, ApiEndpoint.MediaType> content = new HashMap<>();
        
        if (requestBody.getContent() != null) {
            requestBody.getContent().forEach((mediaType, mediaTypeObj) -> {
                content.put(mediaType, convertMediaType(mediaTypeObj));
            });
        }
        
        return ApiEndpoint.RequestBody.builder()
                .description(requestBody.getDescription())
                .required(requestBody.getRequired() != null && requestBody.getRequired())
                .content(content)
                .build();
    }
    
    /**
     * Extract responses
     */
    private Map<Integer, ApiEndpoint.Response> extractResponses(io.swagger.v3.oas.models.responses.ApiResponses responses) {
        Map<Integer, ApiEndpoint.Response> responseMap = new HashMap<>();
        
        if (responses != null) {
            responses.forEach((statusCode, response) -> {
                try {
                    int code = Integer.parseInt(statusCode);
                    responseMap.put(code, convertResponse(response));
                } catch (NumberFormatException e) {
                    // Handle default or other non-numeric status codes
                    log.debug("Skipping non-numeric status code: {}", statusCode);
                }
            });
        }
        
        return responseMap;
    }
    
    /**
     * Convert OpenAPI Response to our model
     */
    private ApiEndpoint.Response convertResponse(ApiResponse response) {
        Map<String, ApiEndpoint.MediaType> content = new HashMap<>();
        
        if (response.getContent() != null) {
            response.getContent().forEach((mediaType, mediaTypeObj) -> {
                content.put(mediaType, convertMediaType(mediaTypeObj));
            });
        }
        
        return ApiEndpoint.Response.builder()
                .description(response.getDescription())
                .content(content)
                .build();
    }
    
    /**
     * Convert OpenAPI MediaType to our model
     */
    private ApiEndpoint.MediaType convertMediaType(MediaType mediaType) {
        ApiEndpoint.Schema schema = null;
        
        if (mediaType.getSchema() != null) {
            schema = convertSchema(mediaType.getSchema());
        }
        
        return ApiEndpoint.MediaType.builder()
                .schema(schema)
                .build();
    }
    
    /**
     * Convert OpenAPI Schema to our model
     */
    private ApiEndpoint.Schema convertSchema(Schema<?> schema) {
        ApiEndpoint.Schema.SchemaBuilder builder = ApiEndpoint.Schema.builder()
                .type(schema.getType())
                .format(schema.getFormat());
        
        if (schema.getRequired() != null) {
            builder.required(schema.getRequired());
        }
        
        if (schema.getProperties() != null) {
            Map<String, ApiEndpoint.Property> properties = new HashMap<>();
            schema.getProperties().forEach((name, prop) -> {
                properties.put(name, convertProperty((Schema<?>) prop));
            });
            builder.properties(properties);
        }
        
        if (schema.getItems() != null) {
            builder.items(convertSchema(schema.getItems()));
        }
        
        if (schema.get$ref() != null) {
            builder.ref(schema.get$ref());
        }
        
        return builder.build();
    }
    
    /**
     * Convert schema property
     */
    private ApiEndpoint.Property convertProperty(Schema<?> prop) {
        return ApiEndpoint.Property.builder()
                .type(prop.getType())
                .format(prop.getFormat())
                .description(prop.getDescription())
                .example(prop.getExample())
                .nullable(prop.getNullable() != null && prop.getNullable())
                .build();
    }
    
    /**
     * Extract required OAuth scopes
     */
    private List<String> extractRequiredScopes(Operation operation) {
        List<String> scopes = new ArrayList<>();
        
        if (operation.getSecurity() != null) {
            operation.getSecurity().forEach(securityRequirement -> {
                securityRequirement.forEach((name, scopeList) -> {
                    scopes.addAll(scopeList);
                });
            });
        }
        
        return scopes;
    }
    
    /**
     * Convert HTTP method
     */
    private ApiEndpoint.HttpMethod convertHttpMethod(PathItem.HttpMethod method) {
        return ApiEndpoint.HttpMethod.valueOf(method.name());
    }
    
    /**
     * Convert parameter location
     */
    private ApiEndpoint.ParameterLocation convertParameterLocation(String in) {
        if (in == null || in.isBlank()) {
            return ApiEndpoint.ParameterLocation.QUERY;
        }

        return switch (in.toLowerCase()) {
            case "path" -> ApiEndpoint.ParameterLocation.PATH;
            case "query" -> ApiEndpoint.ParameterLocation.QUERY;
            case "header" -> ApiEndpoint.ParameterLocation.HEADER;
            case "cookie" -> ApiEndpoint.ParameterLocation.COOKIE;
            default -> ApiEndpoint.ParameterLocation.QUERY;
        };
    }
    
    /**
     * Result of parsing operation
     */
    public record ParseResult(
            boolean success,
            OpenAPI openAPI,
            List<ApiEndpoint> endpoints,
            String errorMessage,
            List<String> warnings
    ) {
        public static ParseResult success(OpenAPI openAPI, List<ApiEndpoint> endpoints) {
            return new ParseResult(true, openAPI, endpoints, null, Collections.emptyList());
        }
        
        public static ParseResult failure(String errorMessage, List<String> warnings) {
            return new ParseResult(false, null, Collections.emptyList(), errorMessage, warnings);
        }
    }
}

// Made with Bob
