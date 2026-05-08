'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import {
  Download,
  AlertTriangle,
  AlertCircle,
  Info,
  ChevronDown,
  ChevronUp,
  PlayCircle,
  Loader2,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import { getValidationProgress, getReportJson, getReportMarkdown, getLatestReport } from '@/lib/api';
import type { Discrepancy, ValidationProgress, ValidationReport } from '@/lib/types';

type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

const SEVERITY_CONFIG: Record<
  Severity,
  { label: string; icon: React.ElementType; color: string; bg: string }
> = {
  CRITICAL: {
    label: 'Critical',
    icon: AlertCircle,
    color: 'text-red-600 dark:text-red-400',
    bg: 'bg-red-50 dark:bg-red-900/20',
  },
  HIGH: {
    label: 'High',
    icon: AlertTriangle,
    color: 'text-orange-600 dark:text-orange-400',
    bg: 'bg-orange-50 dark:bg-orange-900/20',
  },
  MEDIUM: {
    label: 'Medium',
    icon: AlertTriangle,
    color: 'text-yellow-600 dark:text-yellow-400',
    bg: 'bg-yellow-50 dark:bg-yellow-900/20',
  },
  LOW: {
    label: 'Low',
    icon: Info,
    color: 'text-blue-600 dark:text-blue-400',
    bg: 'bg-blue-50 dark:bg-blue-900/20',
  },
  INFO: {
    label: 'Info',
    icon: Info,
    color: 'text-gray-600 dark:text-gray-400',
    bg: 'bg-gray-50 dark:bg-gray-800',
  },
};

function SeverityBadge({ severity }: { severity: Severity }) {
  const cfg = SEVERITY_CONFIG[severity];
  const Icon = cfg.icon;
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ${cfg.bg} ${cfg.color}`}>
      <Icon className="h-3 w-3" />
      {cfg.label}
    </span>
  );
}

function DiscrepancyRow({ item }: { item: Discrepancy }) {
  const [open, setOpen] = useState(false);
  const suggestion = item.suggestedFix ?? item.recommendation;

  return (
    <div className="border-b border-gray-100 last:border-0 dark:border-gray-800">
      <div
        role="button"
        tabIndex={0}
        onClick={() => setOpen((o) => !o)}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setOpen((o) => !o); } }}
        className="flex w-full cursor-pointer items-start gap-4 px-6 py-4 text-left hover:bg-gray-50 dark:hover:bg-gray-800/50"
      >
        <div className="mt-0.5 shrink-0">
          <SeverityBadge severity={item.severity} />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-900 dark:text-white">
            {item.title ?? item.type?.replaceAll('_', ' ') ?? 'Issue'}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {item.endpointPath}
          </p>
        </div>
        {open ? (
          <ChevronUp className="h-4 w-4 shrink-0 text-gray-400" />
        ) : (
          <ChevronDown className="h-4 w-4 shrink-0 text-gray-400" />
        )}
      </div>
      {open && (
        <div className="border-t border-gray-100 bg-gray-50 px-6 py-4 dark:border-gray-800 dark:bg-gray-800/30">
          {item.description && (
            <p className="mb-3 text-sm text-gray-700 dark:text-gray-300">{item.description}</p>
          )}
          <div className="grid gap-3 text-sm md:grid-cols-2">
            {item.documented && (
              <div>
                <p className="text-xs font-semibold uppercase text-gray-500 dark:text-gray-400">
                  Expected
                </p>
                <p className="mt-1 rounded-md bg-white p-3 text-gray-700 dark:bg-gray-900 dark:text-gray-300">
                  {item.documented}
                </p>
              </div>
            )}
            {item.actual && (
              <div>
                <p className="text-xs font-semibold uppercase text-gray-500 dark:text-gray-400">
                  Actual
                </p>
                <p className="mt-1 rounded-md bg-white p-3 text-gray-700 dark:bg-gray-900 dark:text-gray-300">
                  {item.actual}
                </p>
              </div>
            )}
          </div>
          {suggestion && (
            <div className="rounded-lg border border-green-200 bg-green-50 p-3 dark:border-green-800 dark:bg-green-900/20">
              <p className="text-xs font-semibold text-green-700 dark:text-green-400">
                Suggestion
              </p>
              <p className="mt-1 text-sm text-green-800 dark:text-green-300">{suggestion}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function ReportsPage() {
  const { data: progress, isLoading } = useQuery<ValidationProgress>({
    queryKey: ['validation-progress'],
    queryFn: getValidationProgress,
  });
  const { data: report, isLoading: isReportLoading } = useQuery<ValidationReport | null>({
    queryKey: ['latest-report'],
    queryFn: getLatestReport,
    refetchInterval: progress?.status && progress.status !== 'COMPLETED' ? 5_000 : false,
  });

  const downloadJson = async () => {
    const data = await getReportJson();
    const text = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
    const blob = new Blob([text], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'validation-report.json';
    a.click();
    URL.revokeObjectURL(url);
  };

  const downloadMarkdown = async () => {
    const text = await getReportMarkdown();
    const blob = new Blob([text], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'validation-report.md';
    a.click();
    URL.revokeObjectURL(url);
  };

  if (isLoading || isReportLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 className="h-8 w-8 animate-spin text-brand" />
      </div>
    );
  }

  const hasReport = Boolean(report);
  const summary = report?.summary;
  const discrepancies = report?.allDiscrepancies ?? [];
  const recommendations = report?.recommendations ?? [];
  const validationResults = report?.validationResults ?? [];

  return (
    <div className="space-y-8">
      {/* Title */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">📄 Reports</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Validation results and issue details
          </p>
        </div>
        {hasReport && (
          <div className="flex gap-2">
            <button
              onClick={downloadJson}
              className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800"
            >
              <Download className="h-4 w-4" />
              JSON
            </button>
            <button
              onClick={downloadMarkdown}
              className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800"
            >
              <Download className="h-4 w-4" />
              Markdown
            </button>
          </div>
        )}
      </div>

      {!hasReport ? (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-gray-300 py-24 dark:border-gray-700">
          <AlertCircle className="mb-3 h-10 w-10 text-gray-300 dark:text-gray-600" />
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {progress?.status === 'RUNNING'
              ? 'Validation is running — check back shortly.'
              : 'No completed validation report available.'}
          </p>
          <Link
            href="/validation/run"
            className="mt-4 flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark"
          >
            <PlayCircle className="h-4 w-4" />
            Run a Validation
          </Link>
        </div>
      ) : (
        <>
          {/* Summary cards */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {(
              [
                { label: 'Tests Run', value: summary?.totalTests ?? 0, color: 'text-gray-900 dark:text-white' },
                { label: 'Passed', value: summary?.passedTests ?? 0, color: 'text-green-600 dark:text-green-400' },
                { label: 'Failed', value: summary?.failedTests ?? 0, color: 'text-red-600 dark:text-red-400' },
                {
                  label: 'Pass Rate',
          value: `${Math.round(summary?.passRate ?? (summary?.totalTests ? (summary.passedTests / summary.totalTests) * 100 : 0))}%`,
                  color: 'text-brand',
                },
              ] as { label: string; value: string | number; color: string }[]
            ).map(({ label, value, color }) => (
              <div
                key={label}
                className="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-800 dark:bg-gray-900"
              >
                <p className="text-xs font-medium text-gray-500 dark:text-gray-400">{label}</p>
                <p className={`mt-1 text-xl font-bold ${color}`}>{value}</p>
              </div>
            ))}
          </div>

          {/* Timing */}
          {report?.generatedAt && (
            <div className="rounded-xl border border-gray-200 bg-white px-6 py-4 dark:border-gray-800 dark:bg-gray-900">
              <div className="flex flex-wrap gap-8 text-sm">
                <div>
                  <p className="text-gray-500 dark:text-gray-400">Generated</p>
                  <p className="font-medium text-gray-900 dark:text-white" suppressHydrationWarning>
                    {new Date(report.generatedAt).toLocaleString()}
                  </p>
                </div>
                <div>
                  <p className="text-gray-500 dark:text-gray-400">Discrepancies</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {summary?.totalDiscrepancies ?? 0}
                  </p>
                </div>
                <div>
                  <p className="text-gray-500 dark:text-gray-400">Endpoints Tested</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {summary?.endpointsTested ?? 0}
                  </p>
                </div>
              </div>
            </div>
          )}

          <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900">
            <div className="border-b border-gray-200 px-6 py-4 dark:border-gray-800">
              <h3 className="font-semibold text-gray-900 dark:text-white">Discrepancies</h3>
            </div>
            {discrepancies.length === 0 ? (
              <div className="flex items-center gap-2 px-6 py-8 text-sm text-green-600 dark:text-green-400">
                <CheckCircle2 className="h-5 w-5" />
                No discrepancies were found.
              </div>
            ) : (
              discrepancies.map((item) => <DiscrepancyRow key={item.id} item={item} />)
            )}
          </div>

          {recommendations.length > 0 && (
            <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900">
              <div className="border-b border-gray-200 px-6 py-4 dark:border-gray-800">
                <h3 className="font-semibold text-gray-900 dark:text-white">Recommendations</h3>
              </div>
              <div className="divide-y divide-gray-100 dark:divide-gray-800">
                {recommendations.map((recommendation) => (
                  <div key={recommendation.id} className="px-6 py-4">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1">
                        <p className="font-medium text-gray-900 dark:text-white">
                          {recommendation.title}
                        </p>
                        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap">
                          {recommendation.description === '[]' || !recommendation.description
                            ? 'Configure an OpenAI or OpenRouter API key in application.yml to get AI-generated recommendations.'
                            : recommendation.description}
                        </p>
                        {/* Affected endpoints: show list or 'All endpoints' when empty */}
                        <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
                          Affected:{' '}
                          {recommendation.affectedEndpoints && recommendation.affectedEndpoints.length > 0
                            ? recommendation.affectedEndpoints.join(', ')
                            : 'All endpoints'}
                        </p>
                      </div>
                      <div className="shrink-0">
                        <SeverityBadge severity={recommendation.severity} />
                      </div>
                    </div>

                    <div className="mt-3 flex flex-wrap items-center gap-2">
                      <span className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700 dark:bg-gray-800 dark:text-gray-300">
                        Priority {recommendation.priority}
                      </span>
                      <span className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700 dark:bg-gray-800 dark:text-gray-300">
                        Effort {recommendation.estimatedEffort}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900">
            <div className="border-b border-gray-200 px-6 py-4 dark:border-gray-800">
              <h3 className="font-semibold text-gray-900 dark:text-white">Validation Results</h3>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 text-left text-xs font-medium uppercase text-gray-500 dark:bg-gray-800 dark:text-gray-400">
                  <tr>
                    <th className="px-6 py-3">Result</th>
                    <th className="px-6 py-3">Test Case</th>
                    <th className="px-6 py-3">Checks</th>
                    <th className="px-6 py-3">Validated</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                  {validationResults.map((result) => (
                    <tr key={result.id}>
                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ${
                            result.passed
                              ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400'
                              : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'
                          }`}
                        >
                          {result.passed ? (
                            <CheckCircle2 className="h-3 w-3" />
                          ) : (
                            <XCircle className="h-3 w-3" />
                          )}
                          {result.passed ? 'Passed' : 'Failed'}
                        </span>
                      </td>
                      <td className="px-6 py-4 font-mono text-xs text-gray-600 dark:text-gray-400">
                        {result.testCaseId}
                      </td>
                      <td className="px-6 py-4 text-gray-600 dark:text-gray-400">
                        {result.metrics?.passedChecks ?? 0}/{result.metrics?.totalChecks ?? 0}
                      </td>
                      <td className="px-6 py-4 text-gray-500 dark:text-gray-400" suppressHydrationWarning>
                        {new Date(result.validatedAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
