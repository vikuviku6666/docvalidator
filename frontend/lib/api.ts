import type {
  DashboardStats,
  ValidationRun,
  ValidationProgress,
  ValidationReport,
  ValidationRequest,
} from './types';

const BASE = '';  // rewrites proxy: /api/* → http://localhost:8080/api/*

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { cache: 'no-store' });
  if (!res.ok) throw new Error(`GET ${path} failed: ${res.status} ${res.statusText}`);
  return res.json() as Promise<T>;
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`POST ${path} failed: ${res.status} ${res.statusText}`);
  return res.json() as Promise<T>;
}

// ─── Dashboard ───────────────────────────────────────────────────────────────

export const getDashboardStats = () =>
  get<DashboardStats>('/api/dashboard/stats');

export const getValidationHistory = () =>
  get<ValidationRun[]>('/api/dashboard/validations');

// ─── Validation Control ───────────────────────────────────────────────────────

export const startValidation = (request: ValidationRequest) =>
  post<{ status: string; message: string }>('/api/v1/validation/start', request);

export const runValidationSync = (request: ValidationRequest) =>
  post<ValidationReport>('/api/v1/validation/run', request);

export const getValidationProgress = () =>
  get<ValidationProgress>('/api/v1/validation/progress');

// ─── Reports ─────────────────────────────────────────────────────────────────

export const getReportJson = async (): Promise<string> => {
  const res = await fetch('/api/v1/validation/report/json');
  if (!res.ok) throw new Error(`Report fetch failed: ${res.status}`);
  return res.text();
};

export const getReportMarkdown = async (): Promise<string> => {
  const res = await fetch('/api/v1/validation/report/markdown');
  if (!res.ok) throw new Error(`Report fetch failed: ${res.status}`);
  return res.text();
};

// ─── Health ───────────────────────────────────────────────────────────────────

export const getHealth = () =>
  get<{ status: string; application: string; timestamp: number }>('/api/health');