package com.docvalidator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of validating a test case.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    private String id;
    private String testCaseId;
    private boolean passed;
    private LocalDateTime validatedAt;
    
    @Builder.Default
    private List<Discrepancy> discrepancies = new ArrayList<>();
    
    private ValidationMetrics metrics;
    private String validatorAgent; // AI agent that performed validation
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationMetrics {
        private Long responseTimeMs;
        private Integer statusCodeMatch;
        private Double schemaMatchPercentage;
        private Integer headerMatchCount;
        private Integer totalChecks;
        private Integer passedChecks;
        private Integer failedChecks;
    }
    
    /**
     * Add a discrepancy to this validation result
     */
    public void addDiscrepancy(Discrepancy discrepancy) {
        if (this.discrepancies == null) {
            this.discrepancies = new ArrayList<>();
        }
        this.discrepancies.add(discrepancy);
        this.passed = false;
    }
    
    /**
     * Check if validation has any critical discrepancies
     */
    public boolean hasCriticalIssues() {
        return discrepancies != null && discrepancies.stream()
                .anyMatch(d -> d.getSeverity() == Discrepancy.Severity.CRITICAL);
    }
    
    /**
     * Get count of discrepancies by severity
     */
    public long getDiscrepancyCountBySeverity(Discrepancy.Severity severity) {
        return discrepancies != null ? discrepancies.stream()
                .filter(d -> d.getSeverity() == severity)
                .count() : 0;
    }
}

// Made with Bob
