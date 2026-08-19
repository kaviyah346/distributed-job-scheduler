'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { DashboardStatsResponse, JobResponse, WorkerResponse, ProjectResponse } from '@/lib/types';
import { StatCard } from '@/components/StatCard';
import { JobStatusBadge } from '@/components/JobStatusBadge';
import { ExecutionLogsModal } from '@/components/ExecutionLogsModal';
import { CreateJobModal } from '@/components/CreateJobModal';
import { CreateQueueModal } from '@/components/CreateQueueModal';
import { CreateProjectModal } from '@/components/CreateProjectModal';
import { useToast } from '@/components/Toast';
import {
  Activity,
  Layers,
  ListTodo,
  CheckCircle2,
  AlertTriangle,
  ShieldAlert,
  Server,
  RefreshCw,
  Plus,
  Send,
  Terminal,
  Clock,
  Zap,
} from 'lucide-react';

export default function DashboardOverviewPage() {
  const { showToast } = useToast();
  const [stats, setStats] = useState<DashboardStatsResponse | null>(null);
  const [recentJobs, setRecentJobs] = useState<JobResponse[]>([]);
  const [workers, setWorkers] = useState<WorkerResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [autoRefresh, setAutoRefresh] = useState<boolean>(true);

  // Modals
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [isJobModalOpen, setIsJobModalOpen] = useState<boolean>(false);
  const [isQueueModalOpen, setIsQueueModalOpen] = useState<boolean>(false);
  const [isProjectModalOpen, setIsProjectModalOpen] = useState<boolean>(false);

  const fetchData = async () => {
    try {
      const [statsData, jobsPage, workersData] = await Promise.all([
        api.getDashboardStats().catch(() => null),
        api.getJobs({ page: 0, size: 10 }).catch(() => ({ content: [] })),
        api.getWorkers().catch(() => []),
      ]);

      if (statsData) setStats(statsData);
      setRecentJobs(jobsPage.content || []);
      setWorkers(workersData);
    } catch (err: any) {
      // quiet fallback in case of connection warmup
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Periodic polling every 4 seconds for live worker and job updates
  useEffect(() => {
    if (!autoRefresh) return;
    const timer = setInterval(() => {
      fetchData();
    }, 4000);
    return () => clearInterval(timer);
  }, [autoRefresh]);

  return (
    <div className="space-y-6">
      {/* Top Banner & Quick Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-2xl glass-panel border border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2.5">
            <Zap className="w-5 h-5 text-blue-400 fill-blue-400/20" />
            System Control Center
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Real-time PostgreSQL atomic worker orchestration and recurring scheduler metrics
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setAutoRefresh(!autoRefresh)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all flex items-center gap-1.5 ${
              autoRefresh
                ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800/80 shadow-sm'
                : 'bg-slate-900 text-slate-400 border-slate-800'
            }`}
            title="Auto-refresh metrics every 4s"
          >
            <span className={`w-2 h-2 rounded-full ${autoRefresh ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`} />
            {autoRefresh ? 'Live Polling ON' : 'Polling Paused'}
          </button>

          <button
            onClick={fetchData}
            disabled={loading}
            className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 transition-colors"
            title="Refresh now"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={() => setIsProjectModalOpen(true)}
            className="px-3 py-1.5 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors flex items-center gap-1.5"
          >
            <Plus className="w-3.5 h-3.5" /> Project
          </button>

          <button
            onClick={() => setIsQueueModalOpen(true)}
            className="px-3 py-1.5 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors flex items-center gap-1.5"
          >
            <Layers className="w-3.5 h-3.5 text-blue-400" /> Queue
          </button>

          <button
            onClick={() => setIsJobModalOpen(true)}
            className="px-4 py-1.5 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all flex items-center gap-1.5"
          >
            <Send className="w-3.5 h-3.5" /> Submit Job
          </button>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3.5">
        <StatCard
          title="Total Jobs"
          value={stats?.totalJobs ?? 0}
          icon={ListTodo}
          color="blue"
        />
        <StatCard
          title="Running Jobs"
          value={stats?.runningJobs ?? 0}
          icon={Activity}
          color="cyan"
          subtitle="Executing in parallel"
        />
        <StatCard
          title="Queued Jobs"
          value={stats?.queuedJobs ?? 0}
          icon={Clock}
          color="amber"
          subtitle="Ready for claim"
        />
        <StatCard
          title="Completed"
          value={stats?.completedJobs ?? 0}
          icon={CheckCircle2}
          color="emerald"
        />
        <StatCard
          title="Failed Retrying"
          value={stats?.failedJobs ?? 0}
          icon={AlertTriangle}
          color="rose"
        />
        <StatCard
          title="Dead-Lettered"
          value={stats?.deadLetteredJobs ?? 0}
          icon={ShieldAlert}
          color="red"
          subtitle="Exhausted retries"
        />
      </div>

      {/* Grid: Worker Node Health + Recent Jobs Stream */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Registered Worker Nodes */}
        <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
              <Server className="w-4 h-4 text-cyan-400" />
              Active Worker Fleet ({workers.length})
            </h3>
            <span className="text-[11px] font-semibold text-slate-500">5s Heartbeat</span>
          </div>

          {workers.length === 0 ? (
            <div className="py-8 text-center text-xs text-slate-500">
              No active worker nodes registered yet.
            </div>
          ) : (
            <div className="space-y-2.5 max-h-80 overflow-y-auto">
              {workers.map((w) => (
                <div
                  key={w.id}
                  className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 text-xs space-y-1.5"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-mono font-semibold text-slate-200 truncate max-w-[170px]">
                      {w.id}
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        w.status === 'ACTIVE'
                          ? 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                          : w.status === 'BUSY'
                          ? 'bg-cyan-950 text-cyan-300 border border-cyan-800 animate-pulse'
                          : 'bg-rose-950 text-rose-400 border border-rose-800'
                      }`}
                    >
                      {w.status}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-slate-400 text-[11px]">
                    <span>Host: {w.hostname}</span>
                    <span className="font-mono">
                      Load: {w.currentJobCount} / {w.maxConcurrency}
                    </span>
                  </div>

                  <div className="text-[10px] text-slate-500 flex justify-between pt-1 border-t border-slate-800/60">
                    <span>Heartbeat: {new Date(w.lastHeartbeatAt).toLocaleTimeString()}</span>
                    <span>Registered: {new Date(w.registeredAt).toLocaleDateString()}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Live Job Activity Stream */}
        <div className="lg:col-span-2 p-5 rounded-2xl glass-panel border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
              <ListTodo className="w-4 h-4 text-blue-400" />
              Recent Jobs Activity
            </h3>
            <span className="text-[11px] text-slate-400">Top 10 most recent jobs</span>
          </div>

          {recentJobs.length === 0 ? (
            <div className="py-12 text-center text-xs text-slate-500">
              No jobs submitted yet. Click <strong>Submit Job</strong> above to launch your first job!
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-800 text-slate-400 uppercase text-[10px] tracking-wider">
                    <th className="pb-2.5 font-medium">Job Type</th>
                    <th className="pb-2.5 font-medium">Queue</th>
                    <th className="pb-2.5 font-medium">Status</th>
                    <th className="pb-2.5 font-medium">Priority</th>
                    <th className="pb-2.5 font-medium">Retries</th>
                    <th className="pb-2.5 font-medium text-right">Logs</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60 font-medium text-slate-300">
                  {recentJobs.map((job) => (
                    <tr key={job.id} className="hover:bg-slate-900/40 transition-colors">
                      <td className="py-3 font-mono font-semibold text-slate-100">
                        {job.jobType}
                        {job.cronExpression && (
                          <span className="ml-1.5 px-1.5 py-0.2 text-[9px] rounded bg-indigo-950 text-indigo-300 border border-indigo-800">
                            CRON
                          </span>
                        )}
                      </td>
                      <td className="py-3 text-slate-400">{job.queueName || 'Default'}</td>
                      <td className="py-3">
                        <JobStatusBadge status={job.status} size="sm" />
                      </td>
                      <td className="py-3 font-mono text-blue-400 font-semibold">{job.priority}</td>
                      <td className="py-3 font-mono text-slate-400">
                        {job.currentRetryCount} / {job.maxRetries}
                      </td>
                      <td className="py-3 text-right">
                        <button
                          onClick={() => setSelectedJobId(job.id)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-[11px] transition-colors"
                        >
                          <Terminal className="w-3 h-3 text-blue-400" />
                          View Logs
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Execution Logs Drawer Modal */}
      {selectedJobId && (
        <ExecutionLogsModal
          jobId={selectedJobId}
          onClose={() => setSelectedJobId(null)}
        />
      )}

      {/* Action Modals */}
      <CreateJobModal
        isOpen={isJobModalOpen}
        onClose={() => setIsJobModalOpen(false)}
        onSuccess={() => {
          fetchData();
          showToast('Job submitted successfully!', 'success');
        }}
      />

      <CreateQueueModal
        isOpen={isQueueModalOpen}
        onClose={() => setIsQueueModalOpen(false)}
        onSuccess={() => {
          fetchData();
          showToast('Queue created successfully!', 'success');
        }}
      />

      <CreateProjectModal
        isOpen={isProjectModalOpen}
        onClose={() => setIsProjectModalOpen(false)}
        onSuccess={(p: ProjectResponse) => {
          fetchData();
          showToast(`Project '${p.name}' created!`, 'success');
        }}
      />
    </div>
  );
}
