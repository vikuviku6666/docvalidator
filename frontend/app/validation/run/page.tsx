'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { PlayCircle, Settings } from 'lucide-react';
import { startValidation } from '@/lib/api';

const DEFAULT_SPEC_URL =
  'https://developer.spotify.com/reference/web-api/open-api-schema.yaml';

export default function RunValidationPage() {
  const router = useRouter();

  const [specUrl, setSpecUrl] = useState(DEFAULT_SPEC_URL);
  const [endpointFilter, setEndpointFilter] = useState('');
  const [options, setOptions] = useState({
    generateEdgeCases: true,
    includeNegative: true,
    validateSchemas: true,
    checkAuth: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toggle = (key: keyof typeof options) =>
    setOptions((prev) => ({ ...prev, [key]: !prev[key] }));

  const handleStart = async () => {
    if (!specUrl.trim()) {
      setError('OpenAPI spec URL is required');
      return;
    }
    setError(null);
    setLoading(true);

    try {
      const paths = endpointFilter
        .split('\n')
        .map((s) => s.trim())
        .filter(Boolean);

      await startValidation({
        openApiUrl: specUrl.trim(),
        endpointPaths: paths.length > 0 ? paths : null,
      });

      router.push('/validation/progress');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to start validation');
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-8">
      {/* Title */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">▶ Run Validation</h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Validate an OpenAPI specification against the live API
        </p>
      </div>

      {/* OpenAPI Spec */}
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
        <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Target API</h3>
        <label className="block">
          <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
            OpenAPI Specification URL
          </span>
          <input
            type="url"
            value={specUrl}
            onChange={(e) => setSpecUrl(e.target.value)}
            placeholder="https://..."
            className="mt-1 block w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand dark:border-gray-700 dark:bg-gray-800 dark:text-white"
          />
        </label>

        <label className="mt-4 block">
          <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Endpoint Filter{' '}
            <span className="font-normal text-gray-400">(optional — one path per line)</span>
          </span>
          <textarea
            value={endpointFilter}
            onChange={(e) => setEndpointFilter(e.target.value)}
            rows={4}
            placeholder={'/v1/tracks/{id}\n/v1/albums/{id}'}
            className="mt-1 block w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-mono shadow-sm focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand dark:border-gray-700 dark:bg-gray-800 dark:text-white"
          />
        </label>
      </div>

      {/* Test Options */}
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
        <h3 className="mb-4 flex items-center gap-2 font-semibold text-gray-900 dark:text-white">
          <Settings className="h-4 w-4" />
          Test Options
        </h3>
        <div className="space-y-3">
          {(
            [
              ['generateEdgeCases', 'Generate edge case tests'],
              ['includeNegative', 'Include negative tests'],
              ['validateSchemas', 'Validate response schemas'],
              ['checkAuth', 'Check authentication requirements'],
            ] as [keyof typeof options, string][]
          ).map(([key, label]) => (
            <label key={key} className="flex cursor-pointer items-center gap-3">
              <input
                type="checkbox"
                checked={options[key]}
                onChange={() => toggle(key)}
                className="h-4 w-4 rounded border-gray-300 text-brand focus:ring-brand"
              />
              <span className="text-sm text-gray-700 dark:text-gray-300">{label}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-400">
          {error}
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3">
        <button
          onClick={handleStart}
          disabled={loading}
          className="flex items-center gap-2 rounded-lg bg-brand px-6 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-brand-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          <PlayCircle className="h-4 w-4" />
          {loading ? 'Starting…' : 'Start Validation'}
        </button>
      </div>
    </div>
  );
}