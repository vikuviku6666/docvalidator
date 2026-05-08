package com.docvalidator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents an API endpoint extracted from OpenAPI specification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpoint {
    
    private String id;
    private String path;
    private HttpMethod method;
    private String operationId;
    private String summary;
    private String description;
    
    private List<Parameter> parameters;
    private RequestBody requestBody;
    private Map<Integer, Response> responses;
    
    private List<String> tags;
    private List<String> requiredScopes;
    private boolean deprecated;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        private String name;
        private ParameterLocation in;
        private String description;
        private boolean required;
        private String type;
        private Object defaultValue;
        private Object example;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestBody {
        private String description;
        private boolean required;
        private Map<String, MediaType> content;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String description;
        private Map<String, MediaType> content;
        private Map<String, Header> headers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaType {
        private Schema schema;
        private Map<String, Object> examples;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schema {
        private String type;
        private String format;
        private List<String> required;
        private Map<String, Property> properties;
        private Schema items;
        private String ref;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Property {
        private String type;
        private String format;
        private String description;
        private Object example;
        private boolean nullable;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        private String description;
        private String type;
    }
    
    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }
    
    public enum ParameterLocation {
        PATH, QUERY, HEADER, COOKIE
    }
}

// Made with Bob
