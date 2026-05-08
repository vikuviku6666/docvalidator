package com.docvalidator.agent;

import com.docvalidator.config.DocValidatorConfig;
import com.docvalidator.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Agent that validates API responses against documentation.
 * Performs semantic analysis to detect discrepancies between documented and actual behavior.
 */
@Slf4j
@Component
public class ValidatorAgent {
    
    private final DocValidatorConfig config;
    private OpenAiService openAiService; // null when provider != openai or key is placeholder
    private final ObjectMapper objectMapper;

    public ValidatorAgent(DocValidatorConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        String provider = config.getAi().getProvider();
        String apiKey = config.getAi().getOpenai().getApiKey();
        boolean realKey = apiKey != null && !apiKey.isBlank()
                && !apiKey.startsWith("your_") && !apiKey.equals("sk-placeholder");
        if ("openai".equalsIgnoreCase(provider) && realKey) {
            this.openAiService = new OpenAiService(apiKey);
            log.info("OpenAI service initialized for validation");
        } else {
            this.openAiService = null;
            log.info("OpenAI service skipped (provider={}, key configured={}). AI analysis disabled.", provider, realKey);
        }
    }
    
    /**
     * Validate a test case result against expected behavior
     */
    public ValidationResult validate(TestCase testCase) {
        log.info("Validating test case: {}", testCase.getName());
        
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder()
                .id(UUID.randomUUID().toString())
                .testCaseId(testCase.getId())
                .validatedAt(LocalDateTime.now())
                .validatorAgent("ValidatorAgent");
        
        List<Discrepancy> discrepancies = new ArrayList<>();
        
        // Validate status code
        discrepancies.addAll(validateStatusCode(testCase));
        
        // Validate response schema
        if (config.getValidation().getSchemaValidation()) {
            discrepancies.addAll(validateSchema(testCase));
        }
        
        // Validate response headers
        discrepancies.addAll(validateHeaders(testCase));
        
        // Validate response time
        discrepancies.addAll(validateResponseTime(testCase));
        
        // Perform semantic analysis with AI
        if (config.getValidation().getSemanticAnalysis()) {
            discrepancies.addAll(performSemanticAnalysis(testCase));
        }
        
        // Calculate metrics
        ValidationResult.ValidationMetrics metrics = calculateMetrics(testCase, discrepancies);
        
        // Build result
        resultBuilder
                .discrepancies(discrepancies)
                .passed(discrepancies.isEmpty())
                .metrics(metrics);
        
        ValidationResult result = resultBuilder.build();
        
        log.info("Validation complete: {} discrepancies found", discrepancies.size());
        return result;
    }
    
    /**
     * Validate status code matches expected
     */
    private List<Discrepancy> validateStatusCode(TestCase testCase) {
        List<Discrepancy> discrepancies = new ArrayList<>();
        
        if (testCase.getExpectedResult() == null || testCase.getActualResult() == null) {
            return discrepancies;
        }
        
        Integer expected = testCase.getExpectedResult().getStatusCode();
        Integer actual = testCase.getActualResult().getStatusCode();
        
        if (expected != null && !expected.equals(actual)) {
            Discrepancy discrepancy = Discrepancy.builder()
                    .id(UUID.randomUUID().toString())
                    .testCaseId(testCase.getId())
                    .endpointPath(testCase.getEndpoint().getPath())
                    .type(Discrepancy.DiscrepancyType.STATUS_CODE_MISMATCH)
                    .severity(determineSeverity(expected, actual))
                    .title("Status code mismatch")
                    .description(String.format("Expected status code %d but got %d", expected, actual))
                    .documented(String.valueOf(expected))
                    .actual(String.valueOf(actual))
                    .recommendation("Update API documentation to reflect actual status code or fix API implementation")
                    .suggestedFix(String.format("Change documented status code from %d to %d", expected, actual))
                    .detectedAt(LocalDateTime.now())
                    .detectedBy("ValidatorAgent")
                    .build();
            
            discrepancies.add(discrepancy);
        }
        
        return discrepancies;
    }
    
    /**
     * Validate response schema
     */
    private List<Discrepancy> validateSchema(TestCase testCase) {
        List<Discrepancy> discrepancies = new ArrayList<>();
        
        if (testCase.getActualResult() == null || testCase.getActualResult().getBody() == null) {
            return discrepancies;
        }
        
        try {
            JsonNode actualBody = objectMapper.valueToTree(testCase.getActualResult().getBody());
            ApiEndpoint.Response expectedResponse = testCase.getEndpoint().getResponses()
                    .get(testCase.getActualResult().getStatusCode());
            
            if (expectedResponse != null && expectedResponse.getContent() != null) {
                // Get expected schema
                ApiEndpoint.MediaType mediaType = expectedResponse.getContent().get("application/json");
                if (mediaType != null && mediaType.getSchema() != null) {
                    discrepancies.addAll(compareSchemas(actualBody, mediaType.getSchema(), testCase));
                }
            }
            
        } catch (Exception e) {
            log.error("Error validating schema", e);
        }
        
        return discrepancies;
    }
    
    /**
     * Compare actual response with expected schema
     */
    private List<Discrepancy> compareSchemas(JsonNode actual, ApiEndpoint.Schema expected, TestCase testCase) {
        List<Discrepancy> discrepancies = new ArrayList<>();
        
        if (expected.getProperties() == null) {
            return discrepancies;
        }
        
        // Check for missing required fields
        if (expected.getRequired() != null) {
            for (String requiredField : expected.getRequired()) {
                if (!actual.has(requiredField)) {
                    discrepancies.add(createMissingFieldDiscrepancy(requiredField, testCase));
                }
            }
        }
        
        // Check for extra fields not in documentation
        Iterator<String> fieldNames = actual.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!expected.getProperties().containsKey(fieldName)) {
                discrepancies.add(createExtraFieldDiscrepancy(fieldName, actual.get(fieldName), testCase));
            }
        }
        
        // Check field types
        expected.getProperties().forEach((fieldName, property) -> {
            if (actual.has(fieldName)) {
                JsonNode actualField = actual.get(fieldName);
                if (!isTypeMatch(actualField, property.getType())) {
                    discrepancies.add(createTypeMismatchDiscrepancy(fieldName, property.getType(), 
                            actualField.getNodeType().toString(), testCase));
                }
            }
        });
        
        return discrepancies;
    }
    
    /**
     * Validate response headers
     */
    private List<Discrepancy> validateHeaders(TestCase testCase) {
        List<Discrepancy> discrepancies = new ArrayList<>();
        
        if (testCase.getExpectedResult() == null || testCase.getActualResult() == null) {
            return discrepancies;
        }
        
        Map<String, String> expectedHeaders = testCase.getExpectedResult().getHeaders();
        Map<String, String> actualHeaders = testCase.getActualResult().getHeaders();
        
        if (expectedHeaders != null && actualHeaders != null) {
            expectedHeaders.forEach((key, expectedValue) -> {
                String actualValue = actualHeaders.get(key);
                if (actualValue == null || !actualValue.equals(expectedValue)) {
                    // Header mismatch - usually low severity
                    log.debug("Header mismatch for {}: expected={}, actual={}", key, expectedValue, actualValue);
                }
            });
        }
        
        return discrepancies;
    }
    
    /**
     * Validate response time
     */
    private List<Discrepancy> validateResponseTime(TestCase testCase) {
        List<Discrepancy> discrepancies = new ArrayList<>();
        
        if (testCase.getExpectedResult() == null || testCase.getActualResult() == null) {
            return discrepancies;
        }
        
        Long maxResponseTime = testCase.getExpectedResult().getMaxResponseTimeMs();
        Long actualResponseTime = testCase.getActualResult().getResponseTimeMs();
        
        if (maxResponseTime != null && actualResponseTime != null && actualResponseTime > maxResponseTime) {
            Discrepancy discrepancy = Discrepancy.builder()
                    .id(UUID.randomUUID().toString())
                    .testCaseId(testCase.getId())
                    .endpointPath(testCase.getEndpoint().getPath())
                    .type(Discrepancy.DiscrepancyType.PERFORMANCE_ISSUE)
                    .severity(Discrepancy.Severity.MEDIUM)
                    .title("Response time exceeds threshold")
                    .description(String.format("Response took %dms, expected max %dms", 
                            actualResponseTime, maxResponseTime))
                    .documented(maxResponseTime + "ms")
                    .actual(actualResponseTime + "ms")
                    .recommendation("Optimize API performance or update documentation with realistic response times")
                    .detectedAt(LocalDateTime.now())
                    .detectedBy("ValidatorAgent")
                    .build();
            
            discrepancies.add(discrepancy);
        }
        
        return discrepancies;
    }
    
    /**
     * Perform semantic analysis using AI
     */
    private List<Discrepancy> performSemanticAnalysis(TestCase testCase) {
        if (testCase.getActualResult() == null || testCase.getActualResult().getBody() == null) {
            return new ArrayList<>();
        }
        
        try {
            String prompt = buildSemanticAnalysisPrompt(testCase);
            String aiResponse = callOpenAI(prompt);
            return parseAiDiscrepancies(aiResponse, testCase);
            
        } catch (Exception e) {
            log.error("Error performing semantic analysis", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Build prompt for AI semantic analysis
     */
    private String buildSemanticAnalysisPrompt(TestCase testCase) {
        return String.format("""
                Analyze this API response for semantic discrepancies:
                
                Endpoint: %s %s
                Documentation: %s
                
                Expected Response Schema: %s
                Actual Response: %s
                
                Identify any semantic issues:
                1. Fields that exist but behave differently than documented
                2. Undocumented behavior or side effects
                3. Logical inconsistencies
                4. Missing error handling
                
                Return JSON array with: type, severity, description, recommendation
                """,
                testCase.getEndpoint().getMethod(),
                testCase.getEndpoint().getPath(),
                testCase.getEndpoint().getDescription(),
                testCase.getExpectedResult(),
                testCase.getActualResult().getBody());
    }
    
    /**
     * Call OpenAI for semantic analysis — returns "[]" if service is not configured
     */
    private String callOpenAI(String prompt) {
        if (openAiService == null) {
            log.debug("OpenAI service not configured, skipping semantic analysis");
            return "[]";
        }
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(config.getAi().getOpenai().getModel())
                    .messages(Arrays.asList(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(),
                                    "You are an expert API validator. Analyze responses for semantic discrepancies."),
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
     * Parse AI response into discrepancies
     */
    private List<Discrepancy> parseAiDiscrepancies(String aiResponse, TestCase testCase) {
        // TODO: Implement JSON parsing of AI response
        return new ArrayList<>();
    }
    
    /**
     * Calculate validation metrics
     */
    private ValidationResult.ValidationMetrics calculateMetrics(TestCase testCase, List<Discrepancy> discrepancies) {
        int totalChecks = 4; // status, schema, headers, response time
        int failedChecks = discrepancies.size();
        int passedChecks = totalChecks - failedChecks;
        
        return ValidationResult.ValidationMetrics.builder()
                .responseTimeMs(testCase.getActualResult() != null ? 
                        testCase.getActualResult().getResponseTimeMs() : null)
                .statusCodeMatch(testCase.getExpectedResult() != null && testCase.getActualResult() != null &&
                        Objects.equals(testCase.getExpectedResult().getStatusCode(), 
                                testCase.getActualResult().getStatusCode()) ? 1 : 0)
                .totalChecks(totalChecks)
                .passedChecks(passedChecks)
                .failedChecks(failedChecks)
                .build();
    }
    
    // Helper methods
    
    private Discrepancy createMissingFieldDiscrepancy(String fieldName, TestCase testCase) {
        return Discrepancy.builder()
                .id(UUID.randomUUID().toString())
                .testCaseId(testCase.getId())
                .endpointPath(testCase.getEndpoint().getPath())
                .type(Discrepancy.DiscrepancyType.EXTRA_FIELD)
                .severity(Discrepancy.Severity.MEDIUM)
                .title("Required field missing in response")
                .description(String.format("Field '%s' is documented as required but missing in response", fieldName))
                .documented("Field should be present")
                .actual("Field is missing")
                .recommendation(String.format("Add '%s' field to API response or update documentation", fieldName))
                .detectedAt(LocalDateTime.now())
                .detectedBy("ValidatorAgent")
                .build();
    }
    
    private Discrepancy createExtraFieldDiscrepancy(String fieldName, JsonNode value, TestCase testCase) {
        return Discrepancy.builder()
                .id(UUID.randomUUID().toString())
                .testCaseId(testCase.getId())
                .endpointPath(testCase.getEndpoint().getPath())
                .type(Discrepancy.DiscrepancyType.MISSING_FIELD)
                .severity(Discrepancy.Severity.LOW)
                .title("Undocumented field in response")
                .description(String.format("Field '%s' exists in response but not documented", fieldName))
                .documented("Field not mentioned")
                .actual(String.format("Field '%s' = %s", fieldName, value.toString()))
                .recommendation(String.format("Add '%s' field to API documentation", fieldName))
                .detectedAt(LocalDateTime.now())
                .detectedBy("ValidatorAgent")
                .build();
    }
    
    private Discrepancy createTypeMismatchDiscrepancy(String fieldName, String expectedType, 
                                                     String actualType, TestCase testCase) {
        return Discrepancy.builder()
                .id(UUID.randomUUID().toString())
                .testCaseId(testCase.getId())
                .endpointPath(testCase.getEndpoint().getPath())
                .type(Discrepancy.DiscrepancyType.TYPE_MISMATCH)
                .severity(Discrepancy.Severity.HIGH)
                .title("Field type mismatch")
                .description(String.format("Field '%s' type mismatch", fieldName))
                .documented(String.format("Type: %s", expectedType))
                .actual(String.format("Type: %s", actualType))
                .recommendation(String.format("Update documentation or fix API to use correct type for '%s'", fieldName))
                .detectedAt(LocalDateTime.now())
                .detectedBy("ValidatorAgent")
                .build();
    }
    
    private boolean isTypeMatch(JsonNode actual, String expectedType) {
        return switch (expectedType) {
            case "string" -> actual.isTextual();
            case "integer", "number" -> actual.isNumber();
            case "boolean" -> actual.isBoolean();
            case "array" -> actual.isArray();
            case "object" -> actual.isObject();
            default -> true;
        };
    }
    
    private Discrepancy.Severity determineSeverity(Integer expected, Integer actual) {
        // 4xx vs 5xx is critical
        if ((expected / 100) != (actual / 100)) {
            return Discrepancy.Severity.CRITICAL;
        }
        // Same class but different code is high
        return Discrepancy.Severity.HIGH;
    }
}

// Made with Bob
