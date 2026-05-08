package com.docvalidator.cli;

import com.docvalidator.agent.ReporterAgent;
import com.docvalidator.model.ValidationReport;
import com.docvalidator.service.ValidationOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Command-line runner for validation
 * Enabled with: --run-validation=true
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "run-validation", havingValue = "true")
public class ValidationRunner implements CommandLineRunner {
    
    private final ValidationOrchestrator orchestrator;
    private final ReporterAgent reporterAgent;
    
    public ValidationRunner(ValidationOrchestrator orchestrator, 
                           ReporterAgent reporterAgent) {
        this.orchestrator = orchestrator;
        this.reporterAgent = reporterAgent;
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=".repeat(80));
        log.info("DocValidator - AI-Powered API Documentation Testing");
        log.info("=".repeat(80));
        
        // Default to Spotify API
        String openApiUrl = "https://developer.spotify.com/reference/web-api/open-api-schema.yml";
        
        // Check for custom URL in args
        for (String arg : args) {
            if (arg.startsWith("--openapi-url=")) {
                openApiUrl = arg.substring("--openapi-url=".length());
            }
        }
        
        log.info("Target API: {}", openApiUrl);
        log.info("");
        
        try {
            // Run validation
            log.info("Starting validation workflow...");
            ValidationReport report = orchestrator.runValidation(openApiUrl);
            
            // Display summary
            displaySummary(report);
            
            // Export reports
            exportReports(report);
            
            log.info("");
            log.info("=".repeat(80));
            log.info("Validation completed successfully!");
            log.info("=".repeat(80));
            
        } catch (Exception e) {
            log.error("Validation failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Display validation summary
     */
    private void displaySummary(ValidationReport report) {
        log.info("");
        log.info("=".repeat(80));
        log.info("VALIDATION SUMMARY");
        log.info("=".repeat(80));
        
        ValidationReport.Summary summary = report.getSummary();
        
        log.info("Total Tests:      {}", summary.getTotalTests());
        log.info("Passed:           {} ({:.1f}%)", 
                summary.getPassedTests(),
                (summary.getPassedTests() * 100.0 / summary.getTotalTests()));
        log.info("Failed:           {}", summary.getFailedTests());
        log.info("");
        log.info("Health Score:     {:.1f}%", report.getHealthScore());
        log.info("");
        log.info("Issues by Severity:");
        log.info("  🔴 Critical:    {}", summary.getCriticalIssues());
        log.info("  🟠 High:        {}", summary.getHighIssues());
        log.info("  🟡 Medium:      {}", summary.getMediumIssues());
        log.info("  🟢 Low:         {}", summary.getLowIssues());
        log.info("  ℹ️  Info:        {}", summary.getInfoIssues());
        log.info("");
        
        // Display top recommendations
        if (!report.getRecommendations().isEmpty()) {
            log.info("Top Recommendations:");
            report.getRecommendations().stream()
                    .limit(3)
                    .forEach(rec -> {
                        log.info("  • {} (Priority: {})", rec.getTitle(), rec.getPriority());
                    });
            log.info("");
        }
    }
    
    /**
     * Export reports to files
     */
    private void exportReports(ValidationReport report) {
        try {
            // Create reports directory
            Path reportsDir = Paths.get("reports");
            Files.createDirectories(reportsDir);
            
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // Export JSON
            String jsonReport = reporterAgent.exportToJson(report);
            Path jsonPath = reportsDir.resolve("validation_report_" + timestamp + ".json");
            Files.writeString(jsonPath, jsonReport);
            log.info("JSON report saved: {}", jsonPath.toAbsolutePath());
            
            // Export Markdown
            String mdReport = reporterAgent.exportToMarkdown(report);
            Path mdPath = reportsDir.resolve("validation_report_" + timestamp + ".md");
            Files.writeString(mdPath, mdReport);
            log.info("Markdown report saved: {}", mdPath.toAbsolutePath());
            
        } catch (IOException e) {
            log.error("Error exporting reports", e);
        }
    }
}

// Made with Bob
