package com.docvalidator.controller;

import com.docvalidator.service.ValidationHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing dashboard statistics and validation history.
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DashboardController {

    private final ValidationHistoryService historyService;

    /**
     * GET /api/dashboard/stats
     * Aggregate totals for the dashboard home-page cards.
     */
    @GetMapping("/stats")
    public ResponseEntity<ValidationHistoryService.DashboardStats> getStats() {
        log.debug("Fetching dashboard stats");
        return ResponseEntity.ok(historyService.getStats());
    }

    /**
     * GET /api/validations
     * List of all recorded validation runs (newest first).
     */
    @GetMapping("/validations")
    public ResponseEntity<List<ValidationHistoryService.ValidationRun>> getValidations() {
        log.debug("Fetching validation history");
        return ResponseEntity.ok(historyService.getHistory());
    }
}