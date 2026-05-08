'use client';

import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { CheckCircle2, XCircle, Loader2, Clock, ArrowLeft, FileText } from 'lucide-react';
import { getValidationProgress } from '@/lib/api';
import type { ValidationProgress } from '@/lib/types';

const STATUS_LABELS: Record<string, string> = {
  PARSING: '📄 Parsing OpenAPI specification…',
  GENERATING_TESTS: '🤖 Generating test cases…',
  EXECUTING_TESTS: '⚡ Executing tests against live API…',
  GENERATING_REPORT: '📊 Generating validation report…',
  COMPLETED: '✅ Validation Complete',
  FAILED: '❌ Validation Failed',
};

function StatusIcon({ status }: { status: string }) {
  if (status === 'COMPLETED') return <CheckCircle2 className="h-5 w-5 text-green-500" />;
  if (status === 'FAILED') return <XCircle className="h-5 w-5 text-red-500" />;
  if (['PARSING', 'GENERATING_TESTS', 'EXECUTING_TESTS', 'GENERATING_REPORT'].includes(status))
    return <Loader2 className="h-5 w-5 animate-spin text-blue-500" />;
  return <Clock className="h-5 w-5 text-gray-400" />;
}

export default function ProgressPage() {
  const { data: progress, isLoading, error } = useQuery<ValidationProgress>({
    queryKey: ['validation-progress'],
    queryFn: getValidationProgress,
    refetchInterval: 2_000,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 className="h-8 w-8 animate-spin text-brand" />
      </div>
    );
  }

  if (error || !progress) {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-400">
          {error instanceof Error ? error.message : 'No validation in progress.'}
        </div>
        <Link
          href="/validation/run"
          className="inline-flex items-center gap-2 text-sm text-brand hover:underline"
        >
          <ArrowLeft className="h-3 w-3" />
          Run a new validation
        </Link>
      </div>
    );
  }

  const pct = Math.round(progress.progress ?? 0);
  const isDone = progress.status === 'COMPLETED' || progress.status === 'FAILED';

  return (
    <div className="mx-auto max-w-2xl space-y-8">
      {/* Title */}
      <div className="flex items-center gap-3">
        <StatusIcon status={progress.status} />
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
            {STATUS_LABELS[progress.status] ?? progress.status}
          </h2>
          {progress.currentStep && progress.currentStep !== STATUS_LABELS[progress.status] && (
            <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">
              {progress.currentStep}
            </p>
          )}
        </div>
      </div>

      {/* Error message */}
      {progress.error && (
        <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-400">
          {progress.error}
        </div>
      )}

      {/* Progress bar */}
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
        <div className="mb-2 flex items-center justify-between text-sm">
          <span className="font-medium text-gray-700 dark:text-gray-300">
            {progress.currentStep || 'Processing…'}
          </span>
          <span className="font-bold text-gray-900 dark:text-white">{pct}%</span>
        </div>
        <div className="h-3 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
          <div
            className={`h-3 rounded-full transition-all duration-500 ${
              progress.status === 'FAILED' ? 'bg-red-500' : 'bg-brand'
            }`}
            style={{ width: `${pct}%` }}
          />
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {[
          { label: 'Endpoints', value: `${progress.processedEndpoints ?? 0}/${progress.totalEndpoints ?? 0}` },
          { label: 'Tests', value: `${progress.executedTests ?? 0}/${progress.totalTests ?? 0}` },
          { label: 'Passed', value: progress.passedTests ?? 0, green: true },
          { label: 'Failed', value: progress.failedTests ?? 0, red: true },
        ].map(({ label, value, green, red }) => (
          <div
            key={label}
            className="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-800 dark:bg-gray-900"
          >
            <p className="text-xs text-gray-500 dark:text-gray-400">{label}</p>
            <p
              className={`mt-1 text-xl font-bold ${
                green ? 'text-green-600 dark:text-green-400' : red ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-white'
              }`}
            >
              {value}
            </p>
          </div>
        ))}
      </div>

      {/* Timing */}
      {progress.startTime && (
        <div className="rounded-xl border border-gray-200 bg-white px-6 py-4 dark:border-gray-800 dark:bg-gray-900">
          <div className="flex gap-8 text-sm">
            <div>
              <p className="text-gray-500 dark:text-gray-400">Started</p>
              <p className="font-medium text-gray-900 dark:text-white" suppressHydrationWarning>
                {new Date(progress.startTime).toLocaleTimeString()}
              </p>
            </div>
            {progress.endTime && (
              <div>
                <p className="text-gray-500 dark:text-gray-400">Finished</p>
                <p className="font-medium text-gray-900 dark:text-white" suppressHydrationWarning>
                  {new Date(progress.endTime).toLocaleTimeString()}
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Actions when done */}
      {isDone && (
        <div className="flex flex-wrap gap-3">
          <Link
            href="/reports"
            className="flex items-center gap-2 rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-white hover:bg-brand-dark"
          >
            <FileText className="h-4 w-4" />
            View Report
          </Link>
          <Link
            href="/validation/run"
            className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800"
          >
            <ArrowLeft className="h-4 w-4" />
            Run Again
          </Link>
        </div>
      )}
    </div>
  );
}