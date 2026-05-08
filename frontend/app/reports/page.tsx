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
} from 'lucide-react';
import { getValidationProgress, getReportJson, getReportMarkdown } from '@/lib/api';
import type { ValidationProgress } from '@/lib/types';

type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

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

interface DiscrepancyItem {
  id: string;
  type?: string;
  severity: Severity;
  endpoint?: string;
  method?: string;
  description?: string;
  suggestion?: string;
}

function DiscrepancyRow({ item }: { item: DiscrepancyItem }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="border-b border-gray-100 last:border-0 dark:border-gray-800">
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-start gap-4 px-6 py-4 text-left hover:bg-gray-50 dark:hover:bg-gray-800/50"
      >
        <div className="mt-0.5 shrink-0">
          <SeverityBadge severity={item.severity} />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-900 dark:text-white">
            {item.type ?? 'Issue'}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {item.method} {item.endpoint}
          </p>
        </div>
        {open ? (
          <ChevronUp className="h-4 w-4 shrink-0 text-gray-400" />
        ) : (
          <ChevronDown className="h-4 w-4 shrink-0 text-gray-400" />
        )}
      </button>
      {open && (
        <div className="border-t border-gray-100 bg-gray-50 px-6 py-4 dark:border-gray-800 dark:bg-gray-800/30">
          {item.description && (
            <p className="mb-3 text-sm text-gray-700 dark:text-gray-300">{item.description}</p>
          )}
          {item.suggestion && (
            <div className="rounded-lg border border-green-200 bg-green-50 p-3 dark:border-green-800 dark:bg-green-900/20">
              <p className="text-xs font-semibold text-green-700 dark:text-green-400">
                💡 AI Suggestion
              </p>
              <p className="mt-1 text-sm text-green-800 dark:text-green-300">{item.suggestion}</p>
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

  const downloadJson = async () => {
    const text = await getReportJson();
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

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 className="h-8 w-8 animate-spin text-brand" />
      </div>
    );
  }

  const hasReport = progress?.status === 'COMPLETED';

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
                { label: 'Tests Run', value: progress.executedTests ?? 0, color: 'text-gray-900 dark:text-white' },
                { label: 'Passed', value: progress.passedTests ?? 0, color: 'text-green-600 dark:text-green-400' },
                { label: 'Failed', value: progress.failedTests ?? 0, color: 'text-red-600 dark:text-red-400' },
                {
                  label: 'Pass Rate',
                  value: progress.executedTests
                    ? `${Math.round((progress.passedTests / progress.executedTests) * 100)}%`
                    : '—',
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
          {progress.startTime && (
            <div className="rounded-xl border border-gray-200 bg-white px-6 py-4 dark:border-gray-800 dark:bg-gray-900">
              <div className="flex gap-8 text-sm">
                <div>
                  <p className="text-gray-500 dark:text-gray-400">Started</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {new Date(progress.startTime).toLocaleString()}
                  </p>
                </div>
                {progress.endTime && (
                  <div>
                    <p className="text-gray-500 dark:text-gray-400">Completed</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {new Date(progress.endTime).toLocaleString()}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Download prompt */}
          <div className="rounded-xl border border-brand/20 bg-brand/5 p-6">
            <p className="font-medium text-brand">Report complete!</p>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Download the full report as JSON or Markdown using the buttons above.
            </p>
          </div>
        </>
      )}
    </div>
  );
}