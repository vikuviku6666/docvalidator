package com.docvalidator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a complete validation report for an API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReport {
    
    private String id;
    private String apiName;
    private String apiVersion;
    private String openapiSpecUrl;
    
    private LocalDateTime generatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();
    
    @Builder.Default
    private List<ValidationResult> validationResults = new ArrayList<>();
    
    @Builder.Default
    private List<Discrepancy> allDiscrepancies = new ArrayList<>();
    
    private Summary summary;
    
    @Builder.Default
    private List<Recommendation> recommendations = new ArrayList<>();
    
    private Map<Discrepancy.DiscrepancyType, List<Discrepancy>> discrepanciesByType;
    private Map<Discrepancy.Severity, List<Discrepancy>> discrepanciesBySeverity;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long totalTests;
        private Long passedTests;
        private Long failedTests;
        private Long skippedTests;
        
        private Long totalDiscrepancies;
        private Long criticalIssues;
        private Long highIssues;
        private Long mediumIssues;
        private Long lowIssues;
        private Long infoIssues;
        
        private Double passRate;
        private Long averageResponseTimeMs;
        private Integer endpointsTested;
        
        public Double getPassRate() {
            if (totalTests == null || totalTests == 0) {
                return 0.0;
            }
            return (passedTests != null ? passedTests : 0) * 100.0 / totalTests;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String id;
        private String title;
        private String description;
        private Discrepancy.Severity severity;
        private List<String> affectedEndpoints;
        private String estimatedEffort;
        private Integer priority;
    }
    
    /**
     * Calculate and set the summary statistics
     */
    public void calculateSummary() {
        Summary.SummaryBuilder summaryBuilder = Summary.builder();
        
        // Test statistics
        summaryBuilder.totalTests((long) testCases.size());
        summaryBuilder.passedTests(testCases.stream()
                .filter(tc -> tc.getStatus() == TestCase.TestStatus.PASSED)
                .count());
        summaryBuilder.failedTests(testCases.stream()
                .filter(tc -> tc.getStatus() == TestCase.TestStatus.FAILED)
                .count());
        summaryBuilder.skippedTests(testCases.stream()
                .filter(tc -> tc.getStatus() == TestCase.TestStatus.SKIPPED)
                .count());
        
        // Discrepancy statistics
        summaryBuilder.totalDiscrepancies((long) allDiscrepancies.size());
        summaryBuilder.criticalIssues(allDiscrepancies.stream()
                .filter(d -> d.getSeverity() == Discrepancy.Severity.CRITICAL)
                .count());
        summaryBuilder.highIssues(allDiscrepancies.stream()
                .filter(d -> d.getSeverity() == Discrepancy.Severity.HIGH)
                .count());
        summaryBuilder.mediumIssues(allDiscrepancies.stream()
                .filter(d -> d.getSeverity() == Discrepancy.Severity.MEDIUM)
                .count());
        summaryBuilder.lowIssues(allDiscrepancies.stream()
                .filter(d -> d.getSeverity() == Discrepancy.Severity.LOW)
                .count());
        summaryBuilder.infoIssues(allDiscrepancies.stream()
                .filter(d -> d.getSeverity() == Discrepancy.Severity.INFO)
                .count());
        
        // Performance statistics
        summaryBuilder.averageResponseTimeMs((long) testCases.stream()
                .filter(tc -> tc.getExecutionTimeMs() != null)
                .mapToLong(TestCase::getExecutionTimeMs)
                .average()
                .orElse(0.0));
        
        // Endpoint statistics
        summaryBuilder.endpointsTested((int) testCases.stream()
                .map(tc -> tc.getEndpoint().getPath())
                .distinct()
                .count());
        
        this.summary = summaryBuilder.build();
    }
    
    /**
     * Get discrepancies grouped by severity
     */
    public Map<Discrepancy.Severity, List<Discrepancy>> getDiscrepanciesBySeverity() {
        return allDiscrepancies.stream()
                .collect(Collectors.groupingBy(Discrepancy::getSeverity));
    }
    
    /**
     * Get discrepancies grouped by type
     */
    public Map<Discrepancy.DiscrepancyType, List<Discrepancy>> getDiscrepanciesByType() {
        return allDiscrepancies.stream()
                .collect(Collectors.groupingBy(Discrepancy::getType));
    }
    
    /**
     * Check if report has any critical issues
     */
    public boolean hasCriticalIssues() {
        return summary != null && summary.getCriticalIssues() != null && summary.getCriticalIssues() > 0;
    }
    
    /**
     * Get overall health score (0-100)
     */
    public double getHealthScore() {
        if (summary == null || summary.getTotalTests() == 0) {
            return 0.0;
        }
        
        double passRate = summary.getPassRate();
        double discrepancyPenalty = 0.0;
        
        if (summary.getCriticalIssues() != null && summary.getCriticalIssues() > 0) {
            discrepancyPenalty += summary.getCriticalIssues() * 10.0;
        }
        if (summary.getHighIssues() != null && summary.getHighIssues() > 0) {
            discrepancyPenalty += summary.getHighIssues() * 5.0;
        }
        if (summary.getMediumIssues() != null && summary.getMediumIssues() > 0) {
            discrepancyPenalty += summary.getMediumIssues() * 2.0;
        }
        
        return Math.max(0.0, passRate - discrepancyPenalty);
    }
}

// Made with Bob
