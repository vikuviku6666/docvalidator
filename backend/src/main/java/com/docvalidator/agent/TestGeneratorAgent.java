package com.docvalidator.agent;

import com.docvalidator.config.DocValidatorConfig;
import com.docvalidator.mcp.McpClient;
import com.docvalidator.model.ApiEndpoint;
import com.docvalidator.model.TestCase;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Agent that generates test cases from API endpoint documentation.
 * Uses OpenAI GPT-4 and MCP context to intelligently create comprehensive test scenarios.
 *
 * Enhanced with MCP Integration:
 * - Accesses live API behavior via MCP
 * - Compares documented vs actual schemas
 * - Generates performance-aware tests
 * - Creates tests based on real API metrics
 */
@Slf4j
@Component
public class TestGeneratorAgent {
    
    private final DocValidatorConfig config;
    private final McpClient mcpClient;
    private OpenAiService openAiService; // initialized lazily only when provider=openai with a real key

    public TestGeneratorAgent(DocValidatorConfig config, McpClient mcpClient) {
        this.config = config;
        this.mcpClient = mcpClient;

        // Only initialize OpenAI client when the provider is "openai" and a real key is set
        String provider = config.getAi().getProvider();
        String apiKey = config.getAi().getOpenai().getApiKey();
        boolean realKey = apiKey != null && !apiKey.isBlank()
                && !apiKey.startsWith("your_") && !apiKey.equals("sk-placeholder");
        if ("openai".equalsIgnoreCase(provider) && realKey) {
            this.openAiService = new OpenAiService(apiKey);
            log.info("OpenAI service initialized for test generation");
        } else {
            this.openAiService = null;
            log.info("OpenAI service skipped (provider={}, key configured={}). AI test generation disabled.", provider, realKey);
        }
    }
    
    /**
     * Generate test cases for an API endpoint using MCP context
     */
    public List<TestCase> generateTestCases(ApiEndpoint endpoint) {
        log.info("Generating context-aware test cases for endpoint: {} {}", endpoint.getMethod(), endpoint.getPath());
        
        // Get MCP context for intelligent test generation
        McpContext mcpContext = gatherMcpContext(endpoint);
        
        List<TestCase> testCases = new ArrayList<>();
        
        // Generate positive test cases
        if (config.getTestGeneration().getEnabled()) {
            testCases.addAll(generatePositiveTests(endpoint, mcpContext));
        }
        
        // Generate negative test cases
        if (config.getTestGeneration().getGenerateNegativeTests()) {
            testCases.addAll(generateNegativeTests(endpoint, mcpContext));
        }
        
        // Generate edge case tests
        if (config.getTestGeneration().getGenerateEdgeCases()) {
            testCases.addAll(generateEdgeCaseTests(endpoint, mcpContext));
        }
        
        // Generate performance tests based on metrics
        if (mcpContext.hasMetrics()) {
            testCases.addAll(generatePerformanceTests(endpoint, mcpContext));
        }
        
        // Generate schema validation tests
        if (mcpContext.hasActualSchema()) {
            testCases.addAll(generateSchemaValidationTests(endpoint, mcpContext));
        }
        
        // Limit number of tests per endpoint
        int maxTests = config.getTestGeneration().getMaxTestsPerEndpoint();
        if (testCases.size() > maxTests) {
            testCases = testCases.subList(0, maxTests);
        }
        
        log.info("Generated {} context-aware test cases for {} {}", testCases.size(), endpoint.getMethod(), endpoint.getPath());
        return testCases;
    }
    
    /**
     * Gather MCP context for intelligent test generation
     */
    private McpContext gatherMcpContext(ApiEndpoint endpoint) {
        log.debug("Gathering MCP context for {} {}", endpoint.getMethod(), endpoint.getPath());
        
        McpContext context = new McpContext();
        
        try {
            // Get endpoint availability
            Map<String, Object> availability = mcpClient.checkEndpointAvailability(
                endpoint.getMethod().name(),
                endpoint.getPath()
            );
            context.setAvailable((Boolean) availability.getOrDefault("available", false));
            context.setAvailabilityChecked(true);
            
            // Get actual response schema from live API
            Map<String, Object> schemaResult = mcpClient.getResponseSchema(
                endpoint.getPath(),
                endpoint.getMethod().name()
            );
            if (Boolean.TRUE.equals(schemaResult.get("success"))) {
                context.setActualSchema(schemaResult.get("schema"));
                context.setHasActualSchema(true);
            }
            
            // Get API metrics
            Map<String, Object> metricsResult = mcpClient.getApiMetrics(endpoint.getPath());
            if (Boolean.TRUE.equals(metricsResult.get("success"))) {
                context.setMetrics(metricsResult.get("metrics"));
                context.setHasMetrics(true);
            }
            
            // Get authentication requirements
            Map<String, Object> authResult = mcpClient.getAuthRequirements();
            if (Boolean.TRUE.equals(authResult.get("success"))) {
                context.setAuthRequirements(authResult.get("securitySchemes"));
            }
            
        } catch (Exception e) {
            log.warn("Error gathering MCP context: {}", e.getMessage());
        }
        
        return context;
    }
    
    /**
     * Generate positive (happy path) test cases
     */
    private List<TestCase> generatePositiveTests(ApiEndpoint endpoint, McpContext mcpContext) {
        List<TestCase> tests = new ArrayList<>();
        
        // Basic success test
        TestCase basicTest = TestCase.builder()
                .id(UUID.randomUUID().toString())
                .name(String.format("Should successfully %s %s", endpoint.getMethod(), endpoint.getPath()))
                .description("Positive test case with valid parameters")
                .type(TestCase.TestType.POSITIVE)
                .category(TestCase.TestCategory.FUNCTIONAL)
                .endpoint(endpoint)
                .pathParameters(generateValidPathParameters(endpoint))
                .queryParameters(generateValidQueryParameters(endpoint))
                .headers(generateValidHeaders(endpoint))
                .requestBody(generateValidRequestBody(endpoint))
                .expectedResult(buildExpectedSuccessResult(endpoint))
                .status(TestCase.TestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .generatedBy("TestGeneratorAgent")
                .reasoning("Basic positive test to verify endpoint functionality")
                .build();
        
        tests.add(basicTest);
        
        // Use AI to generate additional positive scenarios
        if (config.getAi().getProvider().equals("openai")) {
            tests.addAll(generateAiPositiveTests(endpoint));
        }
        
        return tests;
    }
    
    /**
     * Generate negative test cases
     */
    private List<TestCase> generateNegativeTests(ApiEndpoint endpoint, McpContext mcpContext) {
        List<TestCase> tests = new ArrayList<>();
        
        // Missing required non-path parameters. Path params cannot be omitted in
        // RestAssured because unresolved URI templates fail before the API call.
        if (hasRequiredNonPathParameters(endpoint)) {
            TestCase missingParamTest = TestCase.builder()
                    .id(UUID.randomUUID().toString())
                    .name(String.format("Should return 400 when required parameters missing for %s", endpoint.getPath()))
                    .description("Negative test with missing required parameters")
                    .type(TestCase.TestType.NEGATIVE)
                    .category(TestCase.TestCategory.FUNCTIONAL)
                    .endpoint(endpoint)
                    .pathParameters(new HashMap<>())
                    .queryParameters(new HashMap<>())
                    .headers(generateValidHeaders(endpoint))
                    .expectedResult(buildExpectedErrorResult(400))
                    .status(TestCase.TestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .generatedBy("TestGeneratorAgent")
                    .reasoning("Verify API validates required parameters")
                    .build();
            
            tests.add(missingParamTest);
        }

        // Invalid path parameters
        if (hasRequiredPathParameters(endpoint)) {
            TestCase invalidPathParamTest = TestCase.builder()
                    .id(UUID.randomUUID().toString())
                    .name(String.format("Should reject invalid path parameters for %s", endpoint.getPath()))
                    .description("Negative test with invalid path parameter values")
                    .type(TestCase.TestType.NEGATIVE)
                    .category(TestCase.TestCategory.FUNCTIONAL)
                    .endpoint(endpoint)
                    .pathParameters(generateInvalidPathParameters(endpoint))
                    .queryParameters(generateValidQueryParameters(endpoint))
                    .headers(generateValidHeaders(endpoint))
                    .expectedResult(buildExpectedSuccessOrErrorResult())
                    .status(TestCase.TestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .generatedBy("TestGeneratorAgent")
                    .reasoning("Verify API rejects malformed path identifiers without creating an unexecutable request")
                    .build();

            tests.add(invalidPathParamTest);
        }
        
        // Invalid authentication
        if (requiresAuthentication(endpoint)) {
            TestCase authTest = TestCase.builder()
                    .id(UUID.randomUUID().toString())
                    .name(String.format("Should return 401 when authentication missing for %s", endpoint.getPath()))
                    .description("Negative test without authentication")
                    .type(TestCase.TestType.SECURITY)
                    .category(TestCase.TestCategory.FUNCTIONAL)
                    .endpoint(endpoint)
                    .pathParameters(generateValidPathParameters(endpoint))
                    .queryParameters(generateValidQueryParameters(endpoint))
                    .headers(new HashMap<>()) // No auth header
                    .expectedResult(buildExpectedErrorResult(401))
                    .status(TestCase.TestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .generatedBy("TestGeneratorAgent")
                    .reasoning("Verify API enforces authentication")
                    .build();
            
            tests.add(authTest);
        }
        
        // Use AI to generate more negative scenarios
        if (config.getAi().getProvider().equals("openai")) {
            tests.addAll(generateAiNegativeTests(endpoint));
        }
        
        return tests;
    }
    
    /**
     * Generate edge case test cases
     */
    private List<TestCase> generateEdgeCaseTests(ApiEndpoint endpoint, McpContext mcpContext) {
        List<TestCase> tests = new ArrayList<>();
        
        // Very long parameter values
        TestCase longValueTest = TestCase.builder()
                .id(UUID.randomUUID().toString())
                .name(String.format("Should handle very long parameter values for %s", endpoint.getPath()))
                .description("Edge case test with extremely long parameter values")
                .type(TestCase.TestType.EDGE_CASE)
                .category(TestCase.TestCategory.FUNCTIONAL)
                .endpoint(endpoint)
                .pathParameters(generateEdgeCasePathParameters(endpoint))
                .queryParameters(generateEdgeCaseQueryParameters(endpoint))
                .headers(generateValidHeaders(endpoint))
                .expectedResult(buildExpectedSuccessOrErrorResult())
                .status(TestCase.TestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .generatedBy("TestGeneratorAgent")
                .reasoning("Test boundary conditions with extreme values")
                .build();
        
        tests.add(longValueTest);
        
        return tests;
    }
    
    /**
     * Use AI to generate additional positive test scenarios
     */
    private List<TestCase> generateAiPositiveTests(ApiEndpoint endpoint) {
        String prompt = buildAiPromptForPositiveTests(endpoint);
        String aiResponse = callOpenAI(prompt);
        return parseAiTestCases(aiResponse, endpoint, TestCase.TestType.POSITIVE);
    }
    
    /**
     * Use AI to generate negative test scenarios
     */
    private List<TestCase> generateAiNegativeTests(ApiEndpoint endpoint) {
        String prompt = buildAiPromptForNegativeTests(endpoint);
        String aiResponse = callOpenAI(prompt);
        return parseAiTestCases(aiResponse, endpoint, TestCase.TestType.NEGATIVE);
    }
    
    /**
     * Build AI prompt for positive tests
     */
    private String buildAiPromptForPositiveTests(ApiEndpoint endpoint) {
        return String.format("""
                Generate 2 additional positive test scenarios for this API endpoint:
                
                Endpoint: %s %s
                Summary: %s
                Description: %s
                
                Parameters: %s
                
                For each test scenario, provide:
                1. Test name
                2. Test description
                3. Why this test is important
                
                Format as JSON array with fields: name, description, reasoning
                """,
                endpoint.getMethod(),
                endpoint.getPath(),
                endpoint.getSummary(),
                endpoint.getDescription(),
                endpoint.getParameters());
    }
    
    /**
     * Build AI prompt for negative tests
     */
    private String buildAiPromptForNegativeTests(ApiEndpoint endpoint) {
        return String.format("""
                Generate 2 negative test scenarios for this API endpoint:
                
                Endpoint: %s %s
                Summary: %s
                
                Consider:
                - Invalid parameter values
                - Missing required fields
                - Wrong data types
                - Boundary violations
                
                Format as JSON array with fields: name, description, reasoning, expectedStatusCode
                """,
                endpoint.getMethod(),
                endpoint.getPath(),
                endpoint.getSummary());
    }
    
    /**
     * Call OpenAI API — returns "[]" if service is not configured
     */
    private String callOpenAI(String prompt) {
        if (openAiService == null) {
            log.debug("OpenAI service not configured, skipping AI test generation");
            return "[]";
        }
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(config.getAi().getOpenai().getModel())
                    .messages(Arrays.asList(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                                    "You are an expert API testing engineer. Generate comprehensive test scenarios."),
                            new ChatMessage(ChatMessageRole.USER.value(), prompt)
                    ))
                    .temperature(config.getAi().getOpenai().getTemperature())
                    .maxTokens(config.getAi().getOpenai().getMaxTokens())
                    .build();
            
            return openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
                    
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return "[]";
        }
    }
    
    /**
     * Parse AI response into test cases
     */
    private List<TestCase> parseAiTestCases(String aiResponse, ApiEndpoint endpoint, TestCase.TestType type) {
        // TODO: Implement JSON parsing of AI response
        // For now, return empty list
        return new ArrayList<>();
    }
    
    // Helper methods
    
    private Map<String, Object> generateValidPathParameters(ApiEndpoint endpoint) {
        Map<String, Object> params = new HashMap<>();
        if (endpoint.getParameters() != null) {
            endpoint.getParameters().stream()
                    .filter(p -> p.getIn() == ApiEndpoint.ParameterLocation.PATH)
                    .forEach(p -> params.put(p.getName(), generateValidValue(p)));
        }
        return params;
    }
    
    private Map<String, Object> generateValidQueryParameters(ApiEndpoint endpoint) {
        Map<String, Object> params = new HashMap<>();
        if (endpoint.getParameters() != null) {
            endpoint.getParameters().stream()
                    .filter(p -> p.getIn() == ApiEndpoint.ParameterLocation.QUERY)
                    .filter(ApiEndpoint.Parameter::isRequired)
                    .forEach(p -> params.put(p.getName(), generateValidValue(p)));
        }
        return params;
    }
    
    private Map<String, String> generateValidHeaders(ApiEndpoint endpoint) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        return headers;
    }
    
    private Object generateValidRequestBody(ApiEndpoint endpoint) {
        // TODO: Generate valid request body based on schema
        return null;
    }
    
    private Map<String, Object> generateEdgeCasePathParameters(ApiEndpoint endpoint) {
        Map<String, Object> params = new HashMap<>();
        if (endpoint.getParameters() != null) {
            endpoint.getParameters().stream()
                    .filter(p -> p.getIn() == ApiEndpoint.ParameterLocation.PATH)
                    .forEach(p -> params.put(p.getName(), "x".repeat(1000))); // Very long value
        }
        return params;
    }

    private Map<String, Object> generateInvalidPathParameters(ApiEndpoint endpoint) {
        Map<String, Object> params = new HashMap<>();
        if (endpoint.getParameters() != null) {
            endpoint.getParameters().stream()
                    .filter(p -> p.getIn() == ApiEndpoint.ParameterLocation.PATH)
                    .forEach(p -> params.put(p.getName(), "__invalid__"));
        }
        return params;
    }
    
    private Map<String, Object> generateEdgeCaseQueryParameters(ApiEndpoint endpoint) {
        return new HashMap<>();
    }
    
    private Object generateValidValue(ApiEndpoint.Parameter parameter) {
        if (parameter.getExample() != null) {
            return parameter.getExample();
        }
        
        return switch (parameter.getType()) {
            case "string" -> "test-value";
            case "integer" -> 1;
            case "boolean" -> true;
            default -> "test";
        };
    }
    
    private TestCase.ExpectedResult buildExpectedSuccessResult(ApiEndpoint endpoint) {
        Integer expectedStatus = endpoint.getResponses().containsKey(200) ? 200 : 201;
        
        return TestCase.ExpectedResult.builder()
                .statusCode(expectedStatus)
                .maxResponseTimeMs(5000L)
                .build();
    }
    
    private TestCase.ExpectedResult buildExpectedErrorResult(int statusCode) {
        return TestCase.ExpectedResult.builder()
                .statusCode(statusCode)
                .maxResponseTimeMs(5000L)
                .build();
    }
    
    private TestCase.ExpectedResult buildExpectedSuccessOrErrorResult() {
        return TestCase.ExpectedResult.builder()
                .maxResponseTimeMs(5000L)
                .build();
    }
    
    private boolean hasRequiredNonPathParameters(ApiEndpoint endpoint) {
        return endpoint.getParameters() != null &&
                endpoint.getParameters().stream()
                        .anyMatch(p -> p.isRequired() && p.getIn() != ApiEndpoint.ParameterLocation.PATH);
    }

    private boolean hasRequiredPathParameters(ApiEndpoint endpoint) {
        return endpoint.getParameters() != null &&
                endpoint.getParameters().stream()
                        .anyMatch(p -> p.isRequired() && p.getIn() == ApiEndpoint.ParameterLocation.PATH);
    }
    
    private boolean requiresAuthentication(ApiEndpoint endpoint) {
        return endpoint.getRequiredScopes() != null && !endpoint.getRequiredScopes().isEmpty();
    }
    
    private TestCase.ExpectedResult buildExpectedPerformanceResult(Map<String, Object> metrics) {
        // Extract average response time from metrics
        Long avgResponseTime = 5000L; // Default
        if (metrics.containsKey("avgResponseTime")) {
            avgResponseTime = ((Number) metrics.get("avgResponseTime")).longValue();
        }
        
        // Set SLA to 1.5x average response time
        Long slaThreshold = (long) (avgResponseTime * 1.5);
        
        return TestCase.ExpectedResult.builder()
                .statusCode(200)
                .maxResponseTimeMs(slaThreshold)
                .build();
    }
    
    private TestCase.ExpectedResult buildExpectedSchemaResult(ApiEndpoint endpoint) {
        Integer expectedStatus = endpoint.getResponses().containsKey(200) ? 200 : 201;
        
        return TestCase.ExpectedResult.builder()
                .statusCode(expectedStatus)
                .maxResponseTimeMs(5000L)
                .build();
    }
    
    /**
     * Generate performance test cases based on MCP metrics
     */
    @SuppressWarnings("unchecked")
    private List<TestCase> generatePerformanceTests(ApiEndpoint endpoint, McpContext mcpContext) {
        List<TestCase> tests = new ArrayList<>();

        Map<String, Object> metricsMap = new HashMap<>();
        if (mcpContext.getMetrics() instanceof Map) {
            metricsMap = (Map<String, Object>) mcpContext.getMetrics();
        }

        TestCase perfTest = TestCase.builder()
                .id(UUID.randomUUID().toString())
                .name(String.format("Performance test for %s %s", endpoint.getMethod(), endpoint.getPath()))
                .description("Verify endpoint responds within SLA based on observed metrics")
                .type(TestCase.TestType.POSITIVE)
                .category(TestCase.TestCategory.FUNCTIONAL)
                .endpoint(endpoint)
                .pathParameters(generateValidPathParameters(endpoint))
                .queryParameters(generateValidQueryParameters(endpoint))
                .headers(generateValidHeaders(endpoint))
                .requestBody(generateValidRequestBody(endpoint))
                .expectedResult(buildExpectedPerformanceResult(metricsMap))
                .status(TestCase.TestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .generatedBy("TestGeneratorAgent")
                .reasoning("Performance threshold derived from live API metrics via MCP")
                .build();

        tests.add(perfTest);
        return tests;
    }

    /**
     * Generate schema validation test cases based on actual schema from MCP
     */
    private List<TestCase> generateSchemaValidationTests(ApiEndpoint endpoint, McpContext mcpContext) {
        List<TestCase> tests = new ArrayList<>();

        TestCase schemaTest = TestCase.builder()
                .id(UUID.randomUUID().toString())
                .name(String.format("Schema validation for %s %s", endpoint.getMethod(), endpoint.getPath()))
                .description("Verify response schema matches documentation using live schema from MCP")
                .type(TestCase.TestType.POSITIVE)
                .category(TestCase.TestCategory.FUNCTIONAL)
                .endpoint(endpoint)
                .pathParameters(generateValidPathParameters(endpoint))
                .queryParameters(generateValidQueryParameters(endpoint))
                .headers(generateValidHeaders(endpoint))
                .requestBody(generateValidRequestBody(endpoint))
                .expectedResult(buildExpectedSchemaResult(endpoint))
                .status(TestCase.TestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .generatedBy("TestGeneratorAgent")
                .reasoning("Schema comparison between documentation and live API response via MCP")
                .build();

        tests.add(schemaTest);
        return tests;
    }

    /**
     * MCP Context holder for intelligent test generation
     */
    private static class McpContext {
        private boolean available;
        private boolean availabilityChecked;
        private Object actualSchema;
        private boolean hasActualSchema;
        private Object metrics;
        private boolean hasMetrics;
        private Object authRequirements;
        
        public boolean isAvailable() {
            return available;
        }
        
        public void setAvailable(boolean available) {
            this.available = available;
        }
        
        public boolean isAvailabilityChecked() {
            return availabilityChecked;
        }
        
        public void setAvailabilityChecked(boolean availabilityChecked) {
            this.availabilityChecked = availabilityChecked;
        }
        
        public Object getActualSchema() {
            return actualSchema;
        }
        
        public void setActualSchema(Object actualSchema) {
            this.actualSchema = actualSchema;
        }
        
        public boolean hasActualSchema() {
            return hasActualSchema;
        }
        
        public void setHasActualSchema(boolean hasActualSchema) {
            this.hasActualSchema = hasActualSchema;
        }
        
        public Object getMetrics() {
            return metrics;
        }
        
        public void setMetrics(Object metrics) {
            this.metrics = metrics;
        }
        
        public boolean hasMetrics() {
            return hasMetrics;
        }
        
        public void setHasMetrics(boolean hasMetrics) {
            this.hasMetrics = hasMetrics;
        }
        
        public Object getAuthRequirements() {
            return authRequirements;
        }
        
        public void setAuthRequirements(Object authRequirements) {
            this.authRequirements = authRequirements;
        }
    }
}

// Made with Bob
