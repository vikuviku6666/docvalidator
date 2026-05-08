'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  CheckCircle2,
  XCircle,
  Loader2,
  ExternalLink,
  RefreshCw,
} from 'lucide-react';
import { getHealth } from '@/lib/api';

interface KeyStatus {
  ok: boolean;
  message: string;
  key: string;
}

interface KeyCheckResult {
  openai: KeyStatus;
  openrouter: KeyStatus;
  spotify: KeyStatus;
  allOk: boolean;
}

async function checkKeys(): Promise<KeyCheckResult> {
  const res = await fetch('/api/debug/check-keys');
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

function KeyRow({ label, status }: { label: string; status: KeyStatus }) {
  return (
    <div className="flex items-center gap-4 rounded-lg px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/50">
      <div className="w-5 shrink-0">
        {status.ok ? (
          <CheckCircle2 className="h-5 w-5 text-green-500" />
        ) : (
          <XCircle className="h-5 w-5 text-red-500" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900 dark:text-white">{label}</p>
        <p className="text-xs text-gray-500 dark:text-gray-400">{status.message}</p>
      </div>
      <span className="shrink-0 font-mono text-xs text-gray-400">{status.key}</span>
    </div>
  );
}

export default function HealthPage() {
  const [checking, setChecking] = useState(false);
  const [keyResult, setKeyResult] = useState<KeyCheckResult | null>(null);
  const [keyError, setKeyError] = useState<string | null>(null);

  const { data, isLoading, error, dataUpdatedAt } = useQuery({
    queryKey: ['api-health'],
    queryFn: getHealth,
    refetchInterval: 10_000,
  });

  const handleCheckKeys = async () => {
    setChecking(true);
    setKeyError(null);
    try {
      const result = await checkKeys();
      setKeyResult(result);
    } catch (e) {
      setKeyError(e instanceof Error ? e.message : 'Failed to check keys');
    } finally {
      setChecking(false);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">⚡ API Health</h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Live status of the Spring Boot backend and API keys
        </p>
      </div>

      {/* Backend status */}
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
        <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Backend Status</h3>
        {isLoading && (
          <div className="flex items-center gap-3 text-gray-500">
            <Loader2 className="h-5 w-5 animate-spin" />
            <span className="text-sm">Checking backend…</span>
          </div>
        )}
        {error && (
          <div className="flex items-center gap-3">
            <XCircle className="h-6 w-6 text-red-500" />
            <div>
              <p className="font-semibold text-red-600 dark:text-red-400">Backend Unreachable</p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Make sure Spring Boot is running on port 8080
              </p>
            </div>
          </div>
        )}
        {data && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <CheckCircle2 className="h-6 w-6 text-green-500" />
              <div>
                <p className="font-semibold text-green-600 dark:text-green-400">Backend Online</p>
                <p className="text-xs text-gray-400">
                  Last checked: {dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString() : '—'}
                </p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 pt-2 sm:grid-cols-3">
              <div>
                <p className="text-xs text-gray-500 dark:text-gray-400">Application</p>
                <p className="font-medium text-gray-900 dark:text-white">{data.application}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500 dark:text-gray-400">Status</p>
                <p className="font-medium text-green-600 dark:text-green-400">{data.status}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500 dark:text-gray-400">Server Time</p>
                <p className="font-medium text-gray-900 dark:text-white">
                  {new Date(data.timestamp).toLocaleTimeString()}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* API Key Checker */}
      <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-800">
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white">API Keys</h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              Test if your OpenAI, OpenRouter, and Spotify keys are valid
            </p>
          </div>
          <button
            onClick={handleCheckKeys}
            disabled={checking}
            className="flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-60"
          >
            {checking ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <RefreshCw className="h-4 w-4" />
            )}
            {checking ? 'Checking…' : 'Test Keys'}
          </button>
        </div>

        {keyError && (
          <div className="px-6 py-4 text-sm text-red-600 dark:text-red-400">{keyError}</div>
        )}

        {!keyResult && !keyError && (
          <div className="px-6 py-8 text-center text-sm text-gray-400">
            Click &quot;Test Keys&quot; to check if your API keys are valid
          </div>
        )}

        {keyResult && (
          <div className="divide-y divide-gray-100 dark:divide-gray-800">
            <KeyRow label="OpenAI" status={keyResult.openai} />
            <KeyRow label="OpenRouter" status={keyResult.openrouter} />
            <KeyRow label="Spotify" status={keyResult.spotify} />
            <div className="px-6 py-3">
              <span
                className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${
                  keyResult.allOk
                    ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                    : 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
                }`}
              >
                {keyResult.allOk ? '✓ All keys valid' : '⚠ Some keys missing or invalid'}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Quick links */}
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
        <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Backend Links</h3>
        <div className="space-y-2">
          {[
            ['API Health', 'http://localhost:8080/api/health'],
            ['Check Keys', 'http://localhost:8080/api/debug/check-keys'],
            ['Config Debug', 'http://localhost:8080/api/debug/config'],
            ['H2 Console', 'http://localhost:8080/h2-console'],
            ['API Info', 'http://localhost:8080/api/info'],
          ].map(([label, url]) => (
            <a
              key={url}
              href={url}
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-between rounded-lg px-4 py-2.5 text-sm hover:bg-gray-50 dark:hover:bg-gray-800"
            >
              <span className="text-gray-700 dark:text-gray-300">{label}</span>
              <div className="flex items-center gap-1 text-xs text-gray-400">
                <span className="hidden sm:inline">{url}</span>
                <ExternalLink className="h-3 w-3" />
              </div>
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}