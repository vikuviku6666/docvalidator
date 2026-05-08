// ─── Dashboard ───────────────────────────────────────────────────────────────

export interface DashboardStats {
  totalRuns: number;
  totalTestsRun: number;
  totalPassed: number;
  totalFailed: number;
  lastRunAt: string | null;
}

export interface ValidationRun {
  id: string;
  name: string;
  total: number;
  passed: number;
  failed: number;
  status: 'COMPLETED' | 'FAILED' | 'RUNNING';
  startedAt: string;
  completedAt: string | null;
}

// ─── Validation Progress ──────────────────────────────────────────────────────

/** Matches ValidationOrchestrator.ValidationProgress on the backend */
export interface ValidationProgress {
  status: string;           // PARSING | GENERATING_TESTS | EXECUTING_TESTS | GENERATING_REPORT | COMPLETED | FAILED
  currentStep: string | null;
  progress: number;         // 0-100
  totalEndpoints: number;
  processedEndpoints: number;
  totalTests: number;
  executedTests: number;
  passedTests: number;
  failedTests: number;
  startTime: string | null; // ISO string (write-dates-as-timestamps: false)
  endTime: string | null;
  error: string | null;
}

// ─── Validation Report ────────────────────────────────────────────────────────

export interface ValidationReport {
  id: string;
  generatedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  durationMs: number | null;
  summary: ReportSummary;
  validationResults: ValidationResult[];
  allDiscrepancies: Discrepancy[];
  recommendations: Recommendation[];
}

export interface ReportSummary {
  totalTests: number;
  passedTests: number;
  failedTests: number;
  skippedTests: number | null;
  totalDiscrepancies: number;
  criticalIssues: number;
  highIssues: number;
  mediumIssues: number;
  lowIssues: number;
  infoIssues: number;
  passRate: number;
  averageResponseTimeMs: number | null;
  endpointsTested: number | null;
}

export interface Discrepancy {
  id: string;
  testCaseId: string;
  type: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
  title: string | null;
  endpointPath: string;
  description: string;
  documented: string | null;
  actual: string | null;
  recommendation: string | null;
  suggestedFix: string | null;
  detectedAt: string | null;
  detectedBy: string | null;
}

export interface Recommendation {
  id: string;
  title: string;
  description: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
  affectedEndpoints: string[];
  estimatedEffort: string;
  priority: number;
}

export interface ValidationResult {
  id: string;
  testCaseId: string;
  passed: boolean;
  validatedAt: string;
  discrepancies: Discrepancy[];
  validatorAgent: string;
  metrics: ValidationMetrics | null;
}

export interface ValidationMetrics {
  responseTimeMs: number | null;
  statusCodeMatch: number | null;
  schemaMatchPercentage: number | null;
  headerMatchCount: number | null;
  totalChecks: number | null;
  passedChecks: number | null;
  failedChecks: number | null;
}

// ─── Validation Request ───────────────────────────────────────────────────────

export interface ValidationRequest {
  openApiUrl: string;
  endpointPaths: string[] | null;
}
