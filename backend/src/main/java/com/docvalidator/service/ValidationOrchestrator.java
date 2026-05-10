package com.docvalidator.service;

import com.docvalidator.agent.ReporterAgent;
import com.docvalidator.agent.TestGeneratorAgent;
import com.docvalidator.engine.TestExecutionEngine;
import com.docvalidator.model.ApiEndpoint;
import com.docvalidator.model.TestCase;
import com.docvalidator.model.ValidationReport;
import com.docvalidator.model.ValidationResult;
import com.docvalidator.parser.OpenApiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Orchestrates the entire validation workflow:
 * 1. Parse OpenAPI specification
 * 2. Generate test cases
 * 3. Execute tests
 * 4. Generate report
 */
@Slf4j
@Service
public class ValidationOrchestrator {
    
    private final OpenApiParser openApiParser;
    private final TestGeneratorAgent testGeneratorAgent;
    private final TestExecutionEngine testExecutionEngine;
    private final ReporterAgent reporterAgent;
    private final ValidationHistoryService historyService;
    
    private ValidationProgress currentProgress;
    private ValidationReport latestReport;
    
    public ValidationOrchestrator(OpenApiParser openApiParser,
                                 TestGeneratorAgent testGeneratorAgent,
                                 TestExecutionEngine testExecutionEngine,
                                 ReporterAgent reporterAgent,
                                 ValidationHistoryService historyService) {
        this.openApiParser = openApiParser;
        this.testGeneratorAgent = testGeneratorAgent;
        this.testExecutionEngine = testExecutionEngine;
        this.reporterAgent = reporterAgent;
        this.historyService = historyService;
    }
    
    /**
     * Run complete validation workflow
     */
    public ValidationReport runValidation(String openApiUrl) {
        return runValidation(openApiUrl, null);
    }
    
    /**
     * Run validation for specific endpoints
     */
    public ValidationReport runValidation(String openApiUrl, List<String> endpointPaths) {
        log.info("Starting validation workflow for: {}", openApiUrl);
        
        currentProgress = new ValidationProgress();
        currentProgress.setStatus("PARSING");
        currentProgress.setStartTime(LocalDateTime.now());
        
        try {
            // Step 1: Parse OpenAPI specification
            log.info("Step 1: Parsing OpenAPI specification...");
            currentProgress.setCurrentStep("Parsing OpenAPI specification");
            OpenApiParser.ParseResult parseResult = openApiParser.parseFromUrl(openApiUrl);
            
            List<ApiEndpoint> endpoints = parseResult.endpoints();
            
            // Filter endpoints if specific paths provided
            if (endpointPaths != null && !endpointPaths.isEmpty()) {
                endpoints = endpoints.stream()
                        .filter(e -> endpointPaths.contains(e.getPath()))
                        .toList();
            }
            
            log.info("Parsed {} endpoints", endpoints.size());
            currentProgress.setTotalEndpoints(endpoints.size());
            
            // Step 2: Generate test cases (parallel processing for speed)
            log.info("Step 2: Generating test cases in parallel...");
            currentProgress.setStatus("GENERATING_TESTS");
            currentProgress.setCurrentStep("Generating test cases");
            
            List<TestCase> allTestCases = new ArrayList<>();
            AtomicInteger processedEndpoints = new AtomicInteger(0);
            
            // Create thread pool for parallel processing (use available processors)
            int parallelism = Math.min(Runtime.getRuntime().availableProcessors(), endpoints.size());
            ExecutorService executor = Executors.newFixedThreadPool(parallelism);
            
            // Make endpoints size final for lambda
            final int totalEndpoints = endpoints.size();
            
            try {
                // Submit all endpoint test generation tasks
                List<Future<List<TestCase>>> futures = endpoints.stream()
                    .map(endpoint -> executor.submit(() -> {
                        log.debug("Generating tests for: {} {}", endpoint.getMethod(), endpoint.getPath());
                        
                        List<TestCase> testCases = testGeneratorAgent.generateTestCases(endpoint);
                        
                        int processed = processedEndpoints.incrementAndGet();
                        currentProgress.setProcessedEndpoints(processed);
                        currentProgress.setProgress((processed * 100.0) / totalEndpoints);
                        
                        log.debug("Generated {} tests for endpoint", testCases.size());
                        return testCases;
                    }))
                    .collect(Collectors.toList());
                
                // Collect all results
                for (Future<List<TestCase>> future : futures) {
                    try {
                        allTestCases.addAll(future.get());
                    } catch (ExecutionException e) {
                        log.error("Error generating tests for endpoint", e.getCause());
                    }
                }
                
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            log.info("Generated {} total test cases using {} parallel threads", allTestCases.size(), parallelism);
            currentProgress.setTotalTests(allTestCases.size());
            
            // Step 3: Execute tests
            log.info("Step 3: Executing tests...");
            currentProgress.setStatus("EXECUTING_TESTS");
            currentProgress.setCurrentStep("Executing tests against live API");
            
            TestExecutionEngine.ExecutionResult executionResult = 
                    testExecutionEngine.executeTests(allTestCases);
            
            log.info("Executed {} tests: {} passed, {} failed", 
                    executionResult.getTotalTests(),
                    executionResult.getPassedTests(),
                    executionResult.getFailedTests());
            
            currentProgress.setExecutedTests(executionResult.getTotalTests());
            currentProgress.setPassedTests((int) executionResult.getPassedTests());
            currentProgress.setFailedTests((int) executionResult.getFailedTests());
            
            // Step 4: Generate report
            log.info("Step 4: Generating validation report...");
            currentProgress.setStatus("GENERATING_REPORT");
            currentProgress.setCurrentStep("Generating comprehensive report with AI recommendations");
            
            ValidationReport report = reporterAgent.generateReport(
                    executionResult.validationResults());
            
            currentProgress.setStatus("COMPLETED");
            currentProgress.setEndTime(LocalDateTime.now());
            currentProgress.setProgress(100.0);
            report.setTestCases(executionResult.testCases());
            report.setStartedAt(executionResult.startTime());
            report.setCompletedAt(executionResult.endTime());
            report.setDurationMs(executionResult.durationMs());
            latestReport = report;
            historyService.recordRun(
                    buildRunName(openApiUrl, endpointPaths),
                    currentProgress.getExecutedTests(),
                    currentProgress.getPassedTests(),
                    currentProgress.getFailedTests(),
                    "COMPLETED",
                    currentProgress.getStartTime(),
                    currentProgress.getEndTime());
            
            log.info("Validation workflow completed successfully");
            log.info("Health Score: {}%", String.format("%.1f", report.getHealthScore()));
            
            return report;
            
        } catch (Exception e) {
            log.error("Error during validation workflow", e);
            currentProgress.setStatus("FAILED");
            currentProgress.setError(e.getMessage());
            currentProgress.setEndTime(LocalDateTime.now());
            historyService.recordRun(
                    buildRunName(openApiUrl, endpointPaths),
                    currentProgress.getExecutedTests(),
                    currentProgress.getPassedTests(),
                    currentProgress.getFailedTests(),
                    "FAILED",
                    currentProgress.getStartTime(),
                    currentProgress.getEndTime());
            throw new ValidationException("Validation workflow failed", e);
        }
    }

    private String buildRunName(String openApiUrl, List<String> endpointPaths) {
        if (endpointPaths != null && !endpointPaths.isEmpty()) {
            return String.format("Validation run (%d endpoint%s)",
                    endpointPaths.size(),
                    endpointPaths.size() == 1 ? "" : "s");
        }

        return openApiUrl == null || openApiUrl.isBlank()
                ? "Validation run"
                : "Validation run";
    }
    
    /**
     * Get current validation progress
     */
    public ValidationProgress getProgress() {
        return currentProgress;
    }

    /**
     * Get the most recently completed validation report.
     */
    public ValidationReport getLatestReport() {
        return latestReport;
    }
    
    /**
     * Validation progress tracking
     */
    public static class ValidationProgress {
        private String status;
        private String currentStep;
        private double progress;
        private int totalEndpoints;
        private int processedEndpoints;
        private int totalTests;
        private int executedTests;
        private int passedTests;
        private int failedTests;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String error;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
        
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
        
        public int getTotalEndpoints() { return totalEndpoints; }
        public void setTotalEndpoints(int totalEndpoints) { this.totalEndpoints = totalEndpoints; }
        
        public int getProcessedEndpoints() { return processedEndpoints; }
        public void setProcessedEndpoints(int processedEndpoints) { 
            this.processedEndpoints = processedEndpoints; 
        }
        
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        
        public int getExecutedTests() { return executedTests; }
        public void setExecutedTests(int executedTests) { this.executedTests = executedTests; }
        
        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }
        
        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * Exception for validation errors
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
        
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// Made with Bob
