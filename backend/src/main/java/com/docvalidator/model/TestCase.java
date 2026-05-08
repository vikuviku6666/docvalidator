package com.docvalidator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a generated test case for API validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
    
    private String id;
    private String name;
    private String description;
    private TestType type;
    private TestCategory category;
    
    private ApiEndpoint endpoint;
    private Map<String, Object> pathParameters;
    private Map<String, Object> queryParameters;
    private Map<String, String> headers;
    private Object requestBody;
    
    private ExpectedResult expectedResult;
    private ActualResult actualResult;
    
    private TestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
    private Long executionTimeMs;
    
    private String generatedBy; // AI agent that generated this test
    private String reasoning; // Why this test was generated
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpectedResult {
        private Integer statusCode;
        private Map<String, String> headers;
        private Object body;
        private String bodySchema;
        private Long maxResponseTimeMs;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActualResult {
        private Integer statusCode;
        private Map<String, String> headers;
        private Object body;
        private Long responseTimeMs;
        private String error;
    }
    
    public enum TestType {
        POSITIVE,           // Happy path test
        NEGATIVE,           // Error case test
        EDGE_CASE,          // Boundary condition test
        WORKFLOW,           // Multi-step workflow test
        PERFORMANCE,        // Response time test
        SECURITY,           // Authentication/authorization test
        SCHEMA_VALIDATION   // Schema compliance test
    }
    
    public enum TestCategory {
        FUNCTIONAL,
        INTEGRATION,
        CONTRACT,
        REGRESSION
    }
    
    public enum TestStatus {
        PENDING,
        RUNNING,
        PASSED,
        FAILED,
        SKIPPED,
        ERROR
    }
}

// Made with Bob
