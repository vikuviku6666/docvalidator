package com.docvalidator.engine;

import com.docvalidator.agent.ValidatorAgent;
import com.docvalidator.auth.SpotifyAuthManager;
import com.docvalidator.config.DocValidatorConfig;
import com.docvalidator.model.TestCase;
import com.docvalidator.model.ValidationResult;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Engine that executes test cases against live APIs.
 * Supports parallel execution and real-time progress tracking.
 */
@Slf4j
@Component
public class TestExecutionEngine {
    
    private final DocValidatorConfig config;
    private final ValidatorAgent validatorAgent;
    private final SpotifyAuthManager authManager;
    private final ExecutorService executorService;
    
    public TestExecutionEngine(DocValidatorConfig config,
                              ValidatorAgent validatorAgent,
                              SpotifyAuthManager authManager) {
        this.config = config;
        this.validatorAgent = validatorAgent;
        this.authManager = authManager;
        this.executorService = Executors.newFixedThreadPool(10);
        
        // Configure RestAssured
        RestAssured.baseURI = config.getTargetApi().getBaseUrl();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    
    /**
     * Execute a list of test cases
     */
    public ExecutionResult executeTests(List<TestCase> testCases) {
        log.info("Starting execution of {} test cases", testCases.size());
        
        LocalDateTime startTime = LocalDateTime.now();
        List<ValidationResult> results = new ArrayList<>();
        List<CompletableFuture<ValidationResult>> futures = new ArrayList<>();
        
        // Execute tests (can be parallel or sequential based on config)
        for (TestCase testCase : testCases) {
            CompletableFuture<ValidationResult> future = CompletableFuture.supplyAsync(
                    () -> executeTest(testCase), executorService);
            futures.add(future);
        }
        
        // Wait for all tests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Collect results
        for (CompletableFuture<ValidationResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.error("Error getting test result", e);
            }
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        
        log.info("Execution complete: {} tests executed", results.size());
        
        return new ExecutionResult(
                testCases,
                results,
                startTime,
                endTime,
                calculateDuration(startTime, endTime)
        );
    }
    
    /**
     * Execute a single test case
     */
    public ValidationResult executeTest(TestCase testCase) {
        log.debug("Executing test: {}", testCase.getName());
        
        try {
            // Update test status
            testCase.setStatus(TestCase.TestStatus.RUNNING);
            testCase.setExecutedAt(LocalDateTime.now());
            
            long startTime = System.currentTimeMillis();
            
            // Build and execute request
            Response response = executeRequest(testCase);
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Capture actual result
            TestCase.ActualResult actualResult = captureActualResult(response, executionTime);
            testCase.setActualResult(actualResult);
            testCase.setExecutionTimeMs(executionTime);
            
            // Validate result
            ValidationResult validationResult = validatorAgent.validate(testCase);
            
            // Update test status
            testCase.setStatus(validationResult.isPassed() ? 
                    TestCase.TestStatus.PASSED : TestCase.TestStatus.FAILED);
            
            log.debug("Test {} completed: {}", testCase.getName(), 
                    validationResult.isPassed() ? "PASSED" : "FAILED");
            
            return validationResult;
            
        } catch (Exception e) {
            log.error("Error executing test: {}", testCase.getName(), e);
            testCase.setStatus(TestCase.TestStatus.ERROR);
            
            // Create error result
            TestCase.ActualResult errorResult = TestCase.ActualResult.builder()
                    .error(e.getMessage())
                    .build();
            testCase.setActualResult(errorResult);
            
            return ValidationResult.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .testCaseId(testCase.getId())
                    .passed(false)
                    .validatedAt(LocalDateTime.now())
                    .validatorAgent("TestExecutionEngine")
                    .build();
        }
    }
    
    /**
     * Execute HTTP request using RestAssured
     */
    private Response executeRequest(TestCase testCase) {
        RequestSpecification request = RestAssured.given();

        // Add headers
        if (testCase.getHeaders() != null) {
            testCase.getHeaders().forEach((name, value) -> {
                if ("Authorization".equalsIgnoreCase(name) && "Bearer ${SPOTIFY_TOKEN}".equals(value)) {
                    request.header(name, authManager.getAuthorizationHeader());
                } else {
                    request.header(name, value);
                }
            });
        }

        // Add OAuth token by default for authenticated endpoints, except for
        // security tests that intentionally omit Authorization.
        if (shouldAddDefaultAuthorization(testCase)) {
            request.header("Authorization", authManager.getAuthorizationHeader());
        }
        
        // Add query parameters
        if (testCase.getQueryParameters() != null) {
            testCase.getQueryParameters().forEach(request::queryParam);
        }
        
        // Add path parameters
        if (testCase.getPathParameters() != null) {
            testCase.getPathParameters().forEach(request::pathParam);
        }
        
        // Add request body
        if (testCase.getRequestBody() != null) {
            request.body(testCase.getRequestBody());
        }
        
        // Set timeout (RestAssured uses milliseconds)
        request.config(io.restassured.config.RestAssuredConfig.config()
                .httpClient(io.restassured.config.HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", config.getTargetApi().getTimeout())
                        .setParam("http.socket.timeout", config.getTargetApi().getTimeout())));
        
        // Execute request based on HTTP method
        String path = testCase.getEndpoint().getPath();
        
        return switch (testCase.getEndpoint().getMethod()) {
            case GET -> request.get(path);
            case POST -> request.post(path);
            case PUT -> request.put(path);
            case DELETE -> request.delete(path);
            case PATCH -> request.patch(path);
            case HEAD -> request.head(path);
            case OPTIONS -> request.options(path);
        };
    }

    private boolean shouldAddDefaultAuthorization(TestCase testCase) {
        if (testCase.getEndpoint().getRequiredScopes() == null ||
                testCase.getEndpoint().getRequiredScopes().isEmpty()) {
            return false;
        }

        boolean hasAuthorizationHeader = testCase.getHeaders() != null &&
                testCase.getHeaders().keySet().stream()
                        .anyMatch(name -> "Authorization".equalsIgnoreCase(name));

        return !hasAuthorizationHeader && testCase.getType() != TestCase.TestType.SECURITY;
    }
    
    /**
     * Capture actual result from response
     */
    private TestCase.ActualResult captureActualResult(Response response, long executionTime) {
        Map<String, String> headers = response.getHeaders().asList().stream()
                .collect(java.util.stream.Collectors.toMap(
                        io.restassured.http.Header::getName,
                        io.restassured.http.Header::getValue,
                        (v1, v2) -> v1
                ));
        
        Object body = null;
        try {
            String contentType = response.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                body = response.getBody().as(Object.class);
            } else {
                body = response.getBody().asString();
            }
        } catch (Exception e) {
            log.warn("Could not parse response body", e);
            body = response.getBody().asString();
        }
        
        return TestCase.ActualResult.builder()
                .statusCode(response.getStatusCode())
                .headers(headers)
                .body(body)
                .responseTimeMs(executionTime)
                .build();
    }
    
    /**
     * Calculate duration between two timestamps
     */
    private long calculateDuration(LocalDateTime start, LocalDateTime end) {
        return java.time.Duration.between(start, end).toMillis();
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Result of test execution
     */
    public record ExecutionResult(
            List<TestCase> testCases,
            List<ValidationResult> validationResults,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationMs
    ) {
        public int getTotalTests() {
            return testCases.size();
        }
        
        public long getPassedTests() {
            return validationResults.stream().filter(ValidationResult::isPassed).count();
        }
        
        public long getFailedTests() {
            return validationResults.stream().filter(r -> !r.isPassed()).count();
        }
        
        public double getPassRate() {
            return getTotalTests() > 0 ? (getPassedTests() * 100.0 / getTotalTests()) : 0.0;
        }
    }
}

// Made with Bob
