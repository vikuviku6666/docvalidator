'use client';

import { useState, useEffect } from 'react';
import { Sun, Moon, ExternalLink } from 'lucide-react';

export default function Header({ title }: { title?: string }) {
  // Lazy initializer reads localStorage at mount (client only)
  const [dark, setDark] = useState<boolean>(
    () => typeof window !== 'undefined' && localStorage.getItem('theme') === 'dark',
  );

  // Sync the <html> class whenever dark changes
  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark);
  }, [dark]);

  const toggleDark = () => {
    const next = !dark;
    setDark(next);
    localStorage.setItem('theme', next ? 'dark' : 'light');
  };

  return (
    <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-6 dark:border-gray-800 dark:bg-gray-900">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-white">
        {title ?? 'DocValidator Dashboard'}
      </h1>

      <div className="flex items-center gap-3">
        <a
          href="http://localhost:8080/h2-console"
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-1 rounded-md px-3 py-1.5 text-xs text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
        >
          H2 Console <ExternalLink className="h-3 w-3" />
        </a>
        <a
          href="http://localhost:8080/api/health"
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-1 rounded-md px-3 py-1.5 text-xs text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
        >
          API <ExternalLink className="h-3 w-3" />
        </a>
        <button
          onClick={toggleDark}
          className="rounded-md p-2 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
          aria-label="Toggle dark mode"
        >
          {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
        </button>
      </div>
    </header>
  );
}