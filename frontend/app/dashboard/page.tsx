'use client';

import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { PlayCircle, FileText, CheckCircle2, XCircle, BarChart3, Clock } from 'lucide-react';
import { getDashboardStats, getValidationHistory } from '@/lib/api';
import type { DashboardStats, ValidationRun } from '@/lib/types';

const EMPTY_STATS: DashboardStats = {
  totalRuns: 0,
  totalTestsRun: 0,
  totalPassed: 0,
  totalFailed: 0,
  lastRunAt: null,
};

// ─── Stat Card ────────────────────────────────────────────────────────────────

function StatCard({
  label,
  value,
  icon: Icon,
  color,
}: {
  label: string;
  value: string | number;
  icon: React.ElementType;
  color: string;
}) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{label}</p>
        <div className={`rounded-lg p-2 ${color}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <p className="mt-3 text-3xl font-bold text-gray-900 dark:text-white">{value}</p>
    </div>
  );
}

// ─── Status Badge ─────────────────────────────────────────────────────────────

function StatusBadge({ status }: { status: ValidationRun['status'] }) {
  const map: Record<ValidationRun['status'], string> = {
    COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    FAILED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
    RUNNING: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  };
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${map[status]}`}>
      {status}
    </span>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const {
    data: stats,
    error: statsError,
    isError: isStatsError,
  } = useQuery<DashboardStats>({
    queryKey: ['dashboard-stats'],
    queryFn: getDashboardStats,
    refetchInterval: 15_000,
  });

  const {
    data: history,
    error: historyError,
    isError: isHistoryError,
  } = useQuery<ValidationRun[]>({
    queryKey: ['validation-history'],
    queryFn: getValidationHistory,
    refetchInterval: 15_000,
  });

  const passRate =
    stats && stats.totalTestsRun > 0
      ? Math.round((stats.totalPassed / stats.totalTestsRun) * 100)
      : 0;
  const dashboardStats = stats ?? EMPTY_STATS;

  return (
    <div className="space-y-8">
      {/* Page title */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">📊 Dashboard</h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Overview of your API documentation validation runs
        </p>
      </div>

      {(isStatsError || isHistoryError) && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          Dashboard data could not be loaded.{' '}
          {statsError instanceof Error
            ? statsError.message
            : historyError instanceof Error
              ? historyError.message
              : 'Check that the backend is running on port 8080.'}
        </div>
      )}

      {/* Stats cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Total Runs"
          value={dashboardStats.totalRuns}
          icon={BarChart3}
          color="bg-purple-100 text-purple-600 dark:bg-purple-900/30 dark:text-purple-400"
        />
        <StatCard
          label="Tests Run"
          value={dashboardStats.totalTestsRun}
          icon={Clock}
          color="bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400"
        />
        <StatCard
          label="Passed"
          value={dashboardStats.totalPassed}
          icon={CheckCircle2}
          color="bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400"
        />
        <StatCard
          label="Failed"
          value={dashboardStats.totalFailed}
          icon={XCircle}
          color="bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400"
        />
      </div>

      {/* Pass rate bar */}
      {stats && stats.totalTestsRun > 0 && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
          <div className="flex items-center justify-between">
            <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Overall Pass Rate</p>
            <p className="text-sm font-bold text-gray-900 dark:text-white">{passRate}%</p>
          </div>
          <div className="mt-3 h-2.5 w-full rounded-full bg-gray-200 dark:bg-gray-700">
            <div
              className="h-2.5 rounded-full bg-brand transition-all"
              style={{ width: `${passRate}%` }}
            />
          </div>
        </div>
      )}

      {/* Recent validations */}
      <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-800">
          <h3 className="font-semibold text-gray-900 dark:text-white">Recent Validations</h3>
        </div>

        {!history || history.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-gray-400">
            <BarChart3 className="mb-3 h-10 w-10" />
            <p className="text-sm">No validation runs yet.</p>
            <Link
              href="/validation/run"
              className="mt-3 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark"
            >
              Run your first validation
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-left text-xs font-medium uppercase text-gray-500 dark:bg-gray-800 dark:text-gray-400">
                <tr>
                  <th className="px-6 py-3">Name</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Passed</th>
                  <th className="px-6 py-3">Total</th>
                  <th className="px-6 py-3">Started</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {history.map((run) => (
                  <tr
                    key={run.id}
                    className="hover:bg-gray-50 dark:hover:bg-gray-800/50"
                  >
                    <td className="px-6 py-4 font-medium text-gray-900 dark:text-white">
                      {run.name}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={run.status} />
                    </td>
                    <td className="px-6 py-4 text-green-600 dark:text-green-400">
                      {run.passed}
                    </td>
                    <td className="px-6 py-4 text-gray-600 dark:text-gray-400">{run.total}</td>
                    <td className="px-6 py-4 text-gray-500 dark:text-gray-400">
                      {new Date(run.startedAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Quick actions */}
      <div>
        <h3 className="mb-3 font-semibold text-gray-900 dark:text-white">Quick Actions</h3>
        <div className="flex flex-wrap gap-3">
          <Link
            href="/validation/run"
            className="flex items-center gap-2 rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-brand-dark"
          >
            <PlayCircle className="h-4 w-4" />
            Run New Validation
          </Link>
          <Link
            href="/reports"
            className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800"
          >
            <FileText className="h-4 w-4" />
            View Reports
          </Link>
        </div>
      </div>
    </div>
  );
}
