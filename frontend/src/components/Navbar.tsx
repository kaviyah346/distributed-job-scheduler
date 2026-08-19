'use client';

import React, { useState, useEffect } from 'react';
import { UserRole, PRESET_USERS, getStoredUser, loginWithKeycloak, getStoredToken } from '@/lib/auth';
import { useToast } from './Toast';
import { Shield, Key, ExternalLink, RefreshCw, Check, UserCircle2 } from 'lucide-react';

export const Navbar: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<{ username: string; role: UserRole }>({ username: 'admin', role: 'ADMIN' });
  const [hasToken, setHasToken] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const { showToast } = useToast();

  useEffect(() => {
    const user = getStoredUser();
    setCurrentUser(user);
    setHasToken(!!getStoredToken());
  }, []);

  const handleRoleChange = async (newRole: UserRole) => {
    setLoading(true);
    const result = await loginWithKeycloak(newRole);
    setLoading(false);

    if (result.success) {
      setCurrentUser({ username: PRESET_USERS[newRole].username, role: newRole });
      setHasToken(true);
      showToast(`Switched user to ${PRESET_USERS[newRole].username} (${newRole})`, 'success');
      // Reload page to refresh all active requests with new token
      window.location.reload();
    } else {
      showToast(`Keycloak login failed: ${result.error}`, 'error');
    }
  };

  const handleQuickLogin = async () => {
    setLoading(true);
    const result = await loginWithKeycloak(currentUser.role);
    setLoading(false);
    if (result.success) {
      setHasToken(true);
      showToast(`Logged in successfully via Keycloak!`, 'success');
      window.location.reload();
    } else {
      showToast(`Keycloak connection error: ${result.error}`, 'error');
    }
  };

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
          href="http://localhost:8080/swagger-ui/index.html"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-300 hover:text-white bg-slate-800/80 hover:bg-slate-700 rounded-lg border border-slate-700/70 transition-colors"
        >
          <ExternalLink className="w-3.5 h-3.5 text-blue-400" />
          Swagger Docs
        </a>

        {/* Keycloak User Switcher */}
        <div className="flex items-center gap-2 bg-slate-900/80 border border-slate-800 rounded-lg p-1">
          <div className="flex items-center gap-1.5 px-2.5 py-1">
            <UserCircle2 className="w-4 h-4 text-cyan-400" />
            <span className="text-xs font-semibold text-slate-200">{currentUser.username}</span>
            <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-slate-800 text-cyan-300 border border-slate-700">
              {currentUser.role}
            </span>
          </div>

          <select
            value={currentUser.role}
            onChange={(e) => handleRoleChange(e.target.value as UserRole)}
            disabled={loading}
            className="text-xs bg-slate-800 text-slate-200 border border-slate-700 rounded px-2 py-1 outline-none focus:border-blue-500 cursor-pointer"
          >
            <option value="ADMIN">Switch: Admin</option>
            <option value="DEVELOPER">Switch: Developer</option>
            <option value="OPERATOR">Switch: Operator</option>
          </select>

          {!hasToken ? (
            <button
              onClick={handleQuickLogin}
              disabled={loading}
              className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium bg-blue-600 hover:bg-blue-500 text-white rounded transition-colors"
              title="Get JWT Token from Keycloak"
            >
              {loading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Key className="w-3.5 h-3.5" />}
              Connect Keycloak
            </button>
          ) : (
            <div className="flex items-center gap-1 px-2 py-1 text-[11px] text-emerald-400 font-medium" title="JWT Active">
              <Check className="w-3.5 h-3.5 text-emerald-400" />
              Auth Ready
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
