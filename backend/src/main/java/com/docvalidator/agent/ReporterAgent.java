package com.docvalidator.agent;

import com.docvalidator.config.DocValidatorConfig;
import com.docvalidator.model.Discrepancy;
import com.docvalidator.model.ValidationReport;
import com.docvalidator.model.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered agent that generates comprehensive validation reports
 * with actionable recommendations for fixing discrepancies.
 */
@Slf4j
@Component
public class ReporterAgent {

        private final DocValidatorConfig config;
        private final AiChatClient aiChatClient;
        private final ObjectMapper objectMapper;

        public ReporterAgent(DocValidatorConfig config, AiChatClient aiChatClient) {
                this.config = config;
                this.aiChatClient = aiChatClient;
                this.objectMapper = new ObjectMapper();
                this.objectMapper.findAndRegisterModules();
        }

        /**
         * Generate comprehensive validation report
         */
        public ValidationReport generateReport(List<ValidationResult> validationResults) {
                log.info("Generating validation report for {} results", validationResults.size());

                LocalDateTime reportTime = LocalDateTime.now();

                // Calculate summary statistics
                ValidationReport.Summary summary = calculateSummary(validationResults);
                List<Discrepancy> allDiscrepancies = validationResults.stream()
                                .flatMap(result -> result.getDiscrepancies().stream())
                                .collect(Collectors.toList());

                // Group discrepancies by type and severity
                Map<Discrepancy.DiscrepancyType, List<Discrepancy>> discrepanciesByType = groupDiscrepanciesByType(
                                validationResults);

                Map<Discrepancy.Severity, List<Discrepancy>> discrepanciesBySeverity = groupDiscrepanciesBySeverity(
                                validationResults);

                // Generate AI recommendations for top issues
                List<ValidationReport.Recommendation> recommendations = generateRecommendations(discrepanciesByType,
                                discrepanciesBySeverity);

                // Build report
                ValidationReport report = ValidationReport.builder()
                                .id(UUID.randomUUID().toString())
                                .generatedAt(reportTime)
                                .summary(summary)
                                .validationResults(validationResults)
                                .allDiscrepancies(allDiscrepancies)
                                .discrepanciesByType(discrepanciesByType)
                                .discrepanciesBySeverity(discrepanciesBySeverity)
                                .recommendations(recommendations)
                                .build();

                log.info("Report generated: {} total tests, {} passed, {} failed, health score: {}%",
                                summary.getTotalTests(), summary.getPassedTests(), summary.getFailedTests(),
                                String.format("%.1f", report.getHealthScore()));

                return report;
        }

        /**
         * Calculate summary statistics
         */
        private ValidationReport.Summary calculateSummary(List<ValidationResult> results) {
                long totalTests = results.size();
                long passedTests = results.stream().filter(ValidationResult::isPassed).count();
                long failedTests = totalTests - passedTests;

                long totalDiscrepancies = results.stream()
                                .mapToLong(r -> r.getDiscrepancies().size())
                                .sum();

                Map<Discrepancy.Severity, Long> discrepanciesBySeverity = results.stream()
                                .flatMap(r -> r.getDiscrepancies().stream())
                                .collect(Collectors.groupingBy(Discrepancy::getSeverity, Collectors.counting()));

                return ValidationReport.Summary.builder()
                                .totalTests(totalTests)
                                .passedTests(passedTests)
                                .failedTests(failedTests)
                                .totalDiscrepancies(totalDiscrepancies)
                                .criticalIssues(discrepanciesBySeverity.getOrDefault(Discrepancy.Severity.CRITICAL, 0L))
                                .highIssues(discrepanciesBySeverity.getOrDefault(Discrepancy.Severity.HIGH, 0L))
                                .mediumIssues(discrepanciesBySeverity.getOrDefault(Discrepancy.Severity.MEDIUM, 0L))
                                .lowIssues(discrepanciesBySeverity.getOrDefault(Discrepancy.Severity.LOW, 0L))
                                .infoIssues(discrepanciesBySeverity.getOrDefault(Discrepancy.Severity.INFO, 0L))
                                .build();
        }

        /**
         * Group discrepancies by type
         */
        private Map<Discrepancy.DiscrepancyType, List<Discrepancy>> groupDiscrepanciesByType(
                        List<ValidationResult> results) {
                return results.stream()
                                .flatMap(r -> r.getDiscrepancies().stream())
                                .collect(Collectors.groupingBy(Discrepancy::getType));
        }

        /**
         * Group discrepancies by severity
         */
        private Map<Discrepancy.Severity, List<Discrepancy>> groupDiscrepanciesBySeverity(
                        List<ValidationResult> results) {
                return results.stream()
                                .flatMap(r -> r.getDiscrepancies().stream())
                                .collect(Collectors.groupingBy(Discrepancy::getSeverity));
        }

        /**
         * Generate AI-powered recommendations for fixing issues
         */
        private List<ValidationReport.Recommendation> generateRecommendations(
                        Map<Discrepancy.DiscrepancyType, List<Discrepancy>> byType,
                        Map<Discrepancy.Severity, List<Discrepancy>> bySeverity) {

                List<ValidationReport.Recommendation> recommendations = new ArrayList<>();

                // Focus on critical and high severity issues
                List<Discrepancy> criticalIssues = bySeverity.getOrDefault(Discrepancy.Severity.CRITICAL,
                                Collections.emptyList());
                List<Discrepancy> highIssues = bySeverity.getOrDefault(Discrepancy.Severity.HIGH,
                                Collections.emptyList());

                List<Discrepancy> topIssues = new ArrayList<>();
                topIssues.addAll(criticalIssues);
                topIssues.addAll(highIssues);

                // Limit to top 10 issues to avoid excessive API calls
                topIssues = topIssues.stream().limit(10).collect(Collectors.toList());

                for (Discrepancy discrepancy : topIssues) {
                        try {
                                ValidationReport.Recommendation recommendation = generateRecommendationForDiscrepancy(
                                                discrepancy);
                                recommendations.add(recommendation);
                        } catch (Exception e) {
                                log.error("Error generating recommendation for discrepancy: {}",
                                                discrepancy.getType(), e);
                        }
                }

                // Generate overall recommendations
                if (!byType.isEmpty()) {
                        ValidationReport.Recommendation overallRecommendation = generateOverallRecommendation(byType,
                                        bySeverity);
                        recommendations.add(0, overallRecommendation);
                }

                return recommendations;
        }

        /**
         * Generate recommendation for a specific discrepancy
         */
        private ValidationReport.Recommendation generateRecommendationForDiscrepancy(
                        Discrepancy discrepancy) {

                String aiResponse = "Configure an OpenAI or OpenRouter API key to get AI-generated recommendations.";
                if (aiChatClient.isConfigured()) {
                        try {
                                String prompt = buildDiscrepancyPrompt(discrepancy);
                                aiResponse = aiChatClient.complete(
                                                "You are an expert API documentation engineer. " +
                                                                "Provide concise, actionable recommendations for fixing "
                                                                +
                                                                "API documentation discrepancies.",
                                                prompt,
                                                300);
                        } catch (Exception e) {
                                log.error("Error generating AI recommendation", e);
                        }
                }

                return ValidationReport.Recommendation.builder()
                                .id(UUID.randomUUID().toString())
                                .title("Fix " + discrepancy.getType().name().replace("_", " "))
                                .description(aiResponse)
                                .severity(discrepancy.getSeverity())
                                .affectedEndpoints(List.of(discrepancy.getEndpointPath()))
                                .estimatedEffort(estimateEffort(discrepancy))
                                .priority(calculatePriority(discrepancy))
                                .build();
        }

        /**
         * Generate overall recommendation
         */
        private ValidationReport.Recommendation generateOverallRecommendation(
                        Map<Discrepancy.DiscrepancyType, List<Discrepancy>> byType,
                        Map<Discrepancy.Severity, List<Discrepancy>> bySeverity) {

                String aiResponse = "Configure an OpenAI or OpenRouter API key to get AI-generated strategic recommendations.";
                if (aiChatClient.isConfigured()) {
                        try {
                                String prompt = buildOverallPrompt(byType, bySeverity);
                                aiResponse = aiChatClient.complete(
                                                "You are an expert API documentation engineer. " +
                                                                "Provide strategic recommendations for improving API documentation quality.",
                                                prompt,
                                                500);
                        } catch (Exception e) {
                                log.error("Error generating overall AI recommendation", e);
                        }
                }

                return ValidationReport.Recommendation.builder()
                                .id(UUID.randomUUID().toString())
                                .title("Overall Documentation Quality Improvement")
                                .description(aiResponse)
                                .severity(Discrepancy.Severity.HIGH)
                                .affectedEndpoints(Collections.emptyList())
                                .estimatedEffort("Medium to High")
                                .priority(1)
                                .build();
        }

        /**
         * Build prompt for specific discrepancy
         */
        private String buildDiscrepancyPrompt(Discrepancy discrepancy) {
                return String.format("""
                                API Documentation Discrepancy Detected:

                                Type: %s
                                Severity: %s
                                Endpoint: %s
                                Description: %s
                                Expected: %s
                                Actual: %s

                                Provide a concise, actionable recommendation for fixing this issue.
                                Include:
                                1. Root cause analysis
                                2. Specific steps to fix
                                3. Best practices to prevent similar issues
                                """,
                                discrepancy.getType(),
                                discrepancy.getSeverity(),
                                discrepancy.getEndpointPath(),
                                discrepancy.getDescription(),
                                discrepancy.getDocumented(),
                                discrepancy.getActual());
        }

        /**
         * Build prompt for overall recommendations
         */
        private String buildOverallPrompt(
                        Map<Discrepancy.DiscrepancyType, List<Discrepancy>> byType,
                        Map<Discrepancy.Severity, List<Discrepancy>> bySeverity) {

                StringBuilder prompt = new StringBuilder();
                prompt.append("API Documentation Validation Summary:\n\n");

                prompt.append("Discrepancies by Type:\n");
                byType.forEach((type, discrepancies) -> prompt
                                .append(String.format("- %s: %d issues\n", type, discrepancies.size())));

                prompt.append("\nDiscrepancies by Severity:\n");
                bySeverity.forEach((severity, discrepancies) -> prompt
                                .append(String.format("- %s: %d issues\n", severity, discrepancies.size())));

                prompt.append("""

                                Provide strategic recommendations for improving overall API documentation quality.
                                Focus on:
                                1. Most critical patterns to address
                                2. Process improvements
                                3. Automation opportunities
                                4. Long-term quality strategies
                                """);

                return prompt.toString();
        }

        /**
         * Estimate effort required to fix discrepancy
         */
        private String estimateEffort(Discrepancy discrepancy) {
                return switch (discrepancy.getType()) {
                        case STATUS_CODE_MISMATCH, MISSING_FIELD, EXTRA_FIELD -> "Low";
                        case SCHEMA_MISMATCH, TYPE_MISMATCH, VALIDATION_ERROR -> "Medium";
                        case AUTHENTICATION_ERROR, MISSING_ERROR_RESPONSE -> "High";
                        case PERFORMANCE_ISSUE -> "Medium to High";
                        case DEPRECATED_ENDPOINT, UNDOCUMENTED_BEHAVIOR -> "Low to Medium";
                        default -> "Medium"; // Ensures exhaustiveness for compilation
                };
        }

        /**
         * Calculate priority (1 = highest)
         */
        private int calculatePriority(Discrepancy discrepancy) {
                return switch (discrepancy.getSeverity()) {
                        case CRITICAL -> 1;
                        case HIGH -> 2;
                        case MEDIUM -> 3;
                        case LOW -> 4;
                        case INFO -> 5;
                        default -> 3; // Ensures exhaustiveness for compilation
                };
        }

        /**
         * Export report to JSON
         */
        public String exportToJson(ValidationReport report) {
                try {
                        return objectMapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(report);
                } catch (Exception e) {
                        log.error("Error exporting report to JSON", e);
                        throw new RuntimeException("Failed to export report", e);
                }
        }

        /**
         * Export report to Markdown
         */
        public String exportToMarkdown(ValidationReport report) {
                StringBuilder md = new StringBuilder();

                md.append("# API Documentation Validation Report\n\n");
                md.append(String.format("**Generated:** %s\n\n", report.getGeneratedAt()));

                // Summary
                ValidationReport.Summary summary = report.getSummary();
                double passRate = summary.getTotalTests() == 0
                                ? 0.0
                                : summary.getPassedTests() * 100.0 / summary.getTotalTests();
                md.append("## Summary\n\n");
                md.append(String.format("- **Total Tests:** %d\n", summary.getTotalTests()));
                md.append(String.format("- **Passed:** %d (%.1f%%)\n",
                                summary.getPassedTests(),
                                passRate));
                md.append(String.format("- **Failed:** %d\n", summary.getFailedTests()));
                md.append(String.format("- **Health Score:** %.1f%%\n\n", report.getHealthScore()));

                // Issues by severity
                md.append("## Issues by Severity\n\n");
                md.append(String.format("- 🔴 **Critical:** %d\n", summary.getCriticalIssues()));
                md.append(String.format("- 🟠 **High:** %d\n", summary.getHighIssues()));
                md.append(String.format("- 🟡 **Medium:** %d\n", summary.getMediumIssues()));
                md.append(String.format("- 🟢 **Low:** %d\n", summary.getLowIssues()));
                md.append(String.format("- ℹ️ **Info:** %d\n\n", summary.getInfoIssues()));

                // Recommendations
                if (!report.getRecommendations().isEmpty()) {
                        md.append("## Recommendations\n\n");
                        for (ValidationReport.Recommendation rec : report.getRecommendations()) {
                                md.append(String.format("### %s\n\n", rec.getTitle()));
                                md.append(String.format("**Priority:** %d | **Effort:** %s\n\n",
                                                rec.getPriority(), rec.getEstimatedEffort()));
                                md.append(rec.getDescription()).append("\n\n");
                        }
                }

                return md.toString();
        }
}

// Made with Bob
