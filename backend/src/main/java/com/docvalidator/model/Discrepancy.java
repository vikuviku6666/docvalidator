package com.docvalidator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a discrepancy found between documentation and actual API behavior.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discrepancy {
    
    private String id;
    private String testCaseId;
    private String endpointPath;
    
    private DiscrepancyType type;
    private Severity severity;
    private String title;
    private String description;
    
    private String documented;
    private String actual;
    
    private String recommendation;
    private String suggestedFix;
    
    private LocalDateTime detectedAt;
    private String detectedBy; // AI agent that detected this
    
    public enum DiscrepancyType {
        STATUS_CODE_MISMATCH("Status code doesn't match documentation"),
        SCHEMA_MISMATCH("Response schema differs from documentation"),
        MISSING_FIELD("Field present in response but not documented"),
        EXTRA_FIELD("Field documented but missing in response"),
        TYPE_MISMATCH("Field type doesn't match documentation"),
        VALIDATION_ERROR("Validation rule not enforced"),
        AUTHENTICATION_ERROR("Authentication requirement differs"),
        MISSING_ERROR_RESPONSE("Error response not documented"),
        PERFORMANCE_ISSUE("Response time exceeds documented threshold"),
        DEPRECATED_ENDPOINT("Endpoint marked deprecated but still works"),
        UNDOCUMENTED_BEHAVIOR("Behavior not mentioned in documentation");
        
        private final String description;
        
        DiscrepancyType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum Severity {
        CRITICAL("Critical - Breaks functionality"),
        HIGH("High - Significant impact"),
        MEDIUM("Medium - Moderate impact"),
        LOW("Low - Minor impact"),
        INFO("Info - Informational only");
        
        private final String description;
        
        Severity(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Generate a human-readable summary of this discrepancy
     */
    public String getSummary() {
        return String.format("[%s] %s: %s", severity, type.getDescription(), title);
    }
    
    /**
     * Check if this is a breaking change
     */
    public boolean isBreakingChange() {
        return severity == Severity.CRITICAL || severity == Severity.HIGH;
    }
}

// Made with Bob
