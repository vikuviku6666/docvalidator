package com.docvalidator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Home controller for basic endpoints
 */
@Controller
public class HomeController {
    
    /**
     * Root endpoint - returns welcome message
     */
    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> home() {
        return ResponseEntity.ok(Map.of(
                "application", "DocValidator",
                "version", "1.0.0-SNAPSHOT",
                "description", "AI-Powered API Documentation Testing Framework",
                "status", "running",
                "endpoints", Map.of(
                        "health", "/api/health",
                        "validation", "/api/v1/validation/*",
                        "start_validation", "POST /api/v1/validation/start",
                        "run_validation", "POST /api/v1/validation/run",
                        "get_progress", "GET /api/v1/validation/progress"
                )
        ));
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "DocValidator",
                "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * API info endpoint
     */
    @GetMapping("/api/info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "DocValidator API",
                "version", "1.0.0-SNAPSHOT",
                "description", "AI-Powered API Documentation Testing Framework",
                "features", new String[]{
                        "OpenAPI Specification Parsing",
                        "AI-Powered Test Generation",
                        "Semantic Validation",
                        "OAuth 2.0 Authentication",
                        "Real-time Progress Tracking",
                        "Comprehensive Reporting"
                },
                "endpoints", Map.of(
                        "POST /api/v1/validation/start", "Start validation (async)",
                        "POST /api/v1/validation/run", "Run validation (sync)",
                        "GET /api/v1/validation/progress", "Get validation progress",
                        "GET /api/v1/validation/report/json", "Export report as JSON",
                        "GET /api/v1/validation/report/markdown", "Export report as Markdown"
                )
        ));
    }
}

// Made with Bob
