package com.docvalidator.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory store for validation run history and dashboard statistics.
 */
@Slf4j
@Service
public class ValidationHistoryService {

    private static final int MAX_HISTORY = 50;

    private final ConcurrentLinkedDeque<ValidationRun> history = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalTestsRun   = new AtomicLong(0);
    private final AtomicLong totalPassed     = new AtomicLong(0);
    private final AtomicLong totalFailed     = new AtomicLong(0);

    /**
     * Record a completed (or failed) validation run.
     */
    public ValidationRun recordRun(String name, int total, int passed, int failed,
                                   String status, LocalDateTime startedAt, LocalDateTime completedAt) {
        ValidationRun run = new ValidationRun();
        run.setId(UUID.randomUUID().toString());
        run.setName(name);
        run.setTotal(total);
        run.setPassed(passed);
        run.setFailed(failed);
        run.setStatus(status);
        run.setStartedAt(startedAt);
        run.setCompletedAt(completedAt);

        history.addFirst(run);
        if (history.size() > MAX_HISTORY) {
            history.pollLast();
        }

        totalTestsRun.addAndGet(total);
        totalPassed.addAndGet(passed);
        totalFailed.addAndGet(failed);

        log.info("Recorded validation run: {} — {}/{} passed", name, passed, total);
        return run;
    }

    /**
     * Return all recorded runs (newest first).
     */
    public List<ValidationRun> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Return aggregate dashboard statistics.
     */
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalRuns((long) history.size());
        stats.setTotalTestsRun(totalTestsRun.get());
        stats.setTotalPassed(totalPassed.get());
        stats.setTotalFailed(totalFailed.get());
        stats.setLastRunAt(history.isEmpty() ? null : history.peekFirst().getStartedAt());
        return stats;
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    @Data
    public static class ValidationRun {
        private String id;
        private String name;
        private int total;
        private int passed;
        private int failed;
        private String status;        // COMPLETED | FAILED | RUNNING
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }

    @Data
    public static class DashboardStats {
        private long totalRuns;
        private long totalTestsRun;
        private long totalPassed;
        private long totalFailed;
        private LocalDateTime lastRunAt;
    }
}