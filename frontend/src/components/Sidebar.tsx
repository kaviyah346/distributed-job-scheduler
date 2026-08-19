'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, ListTodo, Layers, AlertOctagon, RotateCcw } from 'lucide-react';

const NAV_ITEMS = [
  { href: '/', label: 'Overview', icon: LayoutDashboard },
  { href: '/jobs', label: 'Jobs Manager', icon: ListTodo },
  { href: '/queues', label: 'Projects & Queues', icon: Layers },
  { href: '/dlq', label: 'Dead Letter Queue', icon: AlertOctagon },
  { href: '/retry-policies', label: 'Retry Policies', icon: RotateCcw },
];

export const Sidebar: React.FC = () => {
  const pathname = usePathname();

  return (
    <aside className="w-64 border-r border-slate-800/80 bg-slate-950/60 min-h-[calc(100vh-4rem)] p-4 flex flex-col justify-between">
      <div className="space-y-1.5">
        <p className="px-3 text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">Navigation</p>
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href || (item.href !== '/' && pathname.startsWith(item.href));

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                isActive
                  ? 'bg-blue-600/15 text-blue-400 border border-blue-500/30 shadow-lg shadow-blue-500/5 font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-blue-400' : 'text-slate-400'}`} />
              {item.label}
            </Link>
          );
        })}
      </div>

      {/* Engine Status info */}
      <div className="p-3.5 rounded-xl border border-slate-800/80 bg-slate-900/40 text-xs space-y-2">
        <div className="flex items-center justify-between text-slate-400">
          <span>Backend API</span>
          <span className="flex items-center gap-1.5 text-emerald-400 font-medium">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            Port 8080
          </span>
        </div>
        <div className="flex items-center justify-between text-slate-400">
          <span>Keycloak Auth</span>
          <span className="text-cyan-400 font-medium">Port 8180</span>
        </div>
        <div className="flex items-center justify-between text-slate-400">
          <span>Concurrency</span>
          <span className="text-slate-300 font-medium">PostgreSQL Lock</span>
        </div>
      </div>
    </aside>
  );
};
