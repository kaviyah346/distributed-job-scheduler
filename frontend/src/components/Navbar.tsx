'use client';

import React, { useState } from 'react';
import { useAuth } from '@/lib/auth';
import { Shield, ExternalLink, UserCircle2, LogOut, RefreshCw } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const [loggingOut, setLoggingOut] = useState<boolean>(false);

  const handleLogout = async () => {
    setLoggingOut(true);
    await logout();
  };

  const apiHost = process.env.NEXT_PUBLIC_API_BASE_URL
    ? process.env.NEXT_PUBLIC_API_BASE_URL.trim().replace(/\/+$/, '').replace(/\/api(\/v1)?\/?$/, '')
    : 'http://localhost:8080';
  const swaggerUrl = `${apiHost}/swagger-ui/index.html`;

  return (
    <header className="sticky top-0 z-40 w-full h-16 border-b border-slate-800/80 glass-panel flex items-center justify-between px-6">
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-blue-500/20">
          <Shield className="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 className="text-base font-bold text-slate-100 flex items-center gap-2">
            Distributed Job Scheduler
            <span className="text-[10px] uppercase font-semibold px-2 py-0.5 rounded-full bg-blue-900/60 text-blue-300 border border-blue-700/50">
              v1.0
            </span>
          </h1>
          <p className="text-xs text-slate-400">Production Engine • PostgreSQL SKIP LOCKED • Keycloak OIDC</p>
        </div>
      </div>

      <div className="flex items-center gap-3">
        {/* Swagger UI quick link */}
        <a
          href={swaggerUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-300 hover:text-white bg-slate-800/80 hover:bg-slate-700 rounded-lg border border-slate-700/70 transition-colors"
        >
          <ExternalLink className="w-3.5 h-3.5 text-blue-400" />
          Swagger Docs
        </a>

        {/* Authenticated Keycloak User & Logout */}
        {isAuthenticated && user && (
          <div className="flex items-center gap-2 bg-slate-900/80 border border-slate-800 rounded-lg p-1">
            <div className="flex items-center gap-1.5 px-2.5 py-1">
              <UserCircle2 className="w-4 h-4 text-cyan-400" />
              <span className="text-xs font-semibold text-slate-200">{user.username}</span>
              <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-blue-950/80 text-cyan-300 border border-blue-800/60">
                {user.role}
              </span>
            </div>

            <div className="h-4 w-[1px] bg-slate-800" />

            <button
              onClick={handleLogout}
              disabled={loggingOut}
              className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-slate-300 hover:text-rose-300 hover:bg-rose-950/40 rounded transition-colors"
              title="Logout from Keycloak"
            >
              {loggingOut ? (
                <RefreshCw className="w-3.5 h-3.5 animate-spin text-rose-400" />
              ) : (
                <LogOut className="w-3.5 h-3.5 text-rose-400" />
              )}
              Logout
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
