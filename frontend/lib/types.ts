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
  summary: ReportSummary;
  discrepancies: Discrepancy[];
  recommendations: Recommendation[];
}

export interface ReportSummary {
  totalTests: number;
  passedTests: number;
  failedTests: number;
  totalDiscrepancies: number;
  criticalIssues: number;
  highIssues: number;
  mediumIssues: number;
  lowIssues: number;
}

export interface Discrepancy {
  id: string;
  type: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  endpoint: string;
  method: string;
  description: string;
  documentedBehavior: string;
  actualBehavior: string;
  suggestion: string;
}

export interface Recommendation {
  id: string;
  title: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
}

// ─── Validation Request ───────────────────────────────────────────────────────

export interface ValidationRequest {
  openApiUrl: string;
  endpointPaths: string[] | null;
}