package com.docvalidator.controller;

import com.docvalidator.agent.ReporterAgent;
import com.docvalidator.model.ValidationReport;
import com.docvalidator.service.ValidationOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for validation operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validation")
@CrossOrigin(origins = "*")
public class ValidationController {
    
    private final ValidationOrchestrator orchestrator;
    private final ReporterAgent reporterAgent;
    
    public ValidationController(ValidationOrchestrator orchestrator, 
                               ReporterAgent reporterAgent) {
        this.orchestrator = orchestrator;
        this.reporterAgent = reporterAgent;
    }
    
    /**
     * Start validation for OpenAPI specification
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startValidation(
            @RequestBody ValidationRequest request) {
        
        log.info("Starting validation for: {}", request.openApiUrl());
        
        try {
            // Run validation in background thread
            new Thread(() -> {
                try {
                    orchestrator.runValidation(request.openApiUrl(), request.endpointPaths());
                } catch (Exception e) {
                    log.error("Validation failed", e);
                }
            }).start();
            
            return ResponseEntity.ok(Map.of(
                    "status", "STARTED",
                    "message", "Validation started successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error starting validation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", e.getMessage()
                    ));
        }
    }
    
    /**
     * Get validation progress
     */
    @GetMapping("/progress")
    public ResponseEntity<ValidationOrchestrator.ValidationProgress> getProgress() {
        ValidationOrchestrator.ValidationProgress progress = orchestrator.getProgress();
        
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(progress);
    }
    
    /**
     * Run validation synchronously and return report
     */
    @PostMapping("/run")
    public ResponseEntity<ValidationReport> runValidation(
            @RequestBody ValidationRequest request) {
        
        log.info("Running validation for: {}", request.openApiUrl());
        
        try {
            ValidationReport report = orchestrator.runValidation(
                    request.openApiUrl(), 
                    request.endpointPaths());
            
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            log.error("Validation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Export report as JSON
     */
    @GetMapping(value = "/report/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exportReportJson() {
        try {
            ValidationOrchestrator.ValidationProgress progress = orchestrator.getProgress();
            
            if (progress == null || !progress.getStatus().equals("COMPLETED")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Note: In production, you'd store the report and retrieve it here
            return ResponseEntity.ok("{}");
            
        } catch (Exception e) {
            log.error("Error exporting report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Export report as Markdown
     */
    @GetMapping(value = "/report/markdown", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public ResponseEntity<String> exportReportMarkdown() {
        try {
            ValidationOrchestrator.ValidationProgress progress = orchestrator.getProgress();
            
            if (progress == null || !progress.getStatus().equals("COMPLETED")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Note: In production, you'd store the report and retrieve it here
            return ResponseEntity.ok("# Report");
            
        } catch (Exception e) {
            log.error("Error exporting report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Validation request DTO
     */
    public record ValidationRequest(
            String openApiUrl,
            List<String> endpointPaths
    ) {}
}

// Made with Bob
