'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { ProjectResponse, QueueResponse, QueueStatsResponse } from '@/lib/types';
import { CreateQueueModal } from '@/components/CreateQueueModal';
import { CreateProjectModal } from '@/components/CreateProjectModal';
import { CreateJobModal } from '@/components/CreateJobModal';
import { useToast } from '@/components/Toast';
import {
  Layers,
  FolderPlus,
  Plus,
  Play,
  Pause,
  Key,
  Copy,
  Check,
  RefreshCw,
  Send,
  Sliders,
  ShieldCheck,
} from 'lucide-react';

export default function QueuesPage() {
  const { showToast } = useToast();
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [queues, setQueues] = useState<QueueResponse[]>([]);
  const [queueStats, setQueueStats] = useState<Record<string, QueueStatsResponse>>({});
  const [selectedProjectId, setSelectedProjectId] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Modals
  const [isProjectModalOpen, setIsProjectModalOpen] = useState<boolean>(false);
  const [isQueueModalOpen, setIsQueueModalOpen] = useState<boolean>(false);
  const [isJobModalOpen, setIsJobModalOpen] = useState<boolean>(false);
  const [targetQueueForJob, setTargetQueueForJob] = useState<{ projectId: string; queueId: string } | null>(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const [pList, qList] = await Promise.all([
        api.getProjects(),
        api.getQueues(selectedProjectId || undefined),
      ]);
      setProjects(pList);
      setQueues(qList);

      // Fetch stats for all queues in parallel
      const statsMap: Record<string, QueueStatsResponse> = {};
      await Promise.all(
        qList.map(async (q) => {
          try {
            const s = await api.getQueueStats(q.id);
            statsMap[q.id] = s;
          } catch (e) {}
        })
      );
      setQueueStats(statsMap);
    } catch (err: any) {
      showToast(err.message || 'Failed to load projects & queues', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedProjectId]);

  const handleTogglePause = async (queue: QueueResponse) => {
    try {
      if (queue.isPaused) {
        await api.resumeQueue(queue.id);
        showToast(`Queue '${queue.name}' resumed!`, 'success');
      } else {
        await api.pauseQueue(queue.id);
        showToast(`Queue '${queue.name}' paused!`, 'info');
      }
      loadData();
    } catch (err: any) {
      showToast(err.message || 'Failed to toggle queue pause state', 'error');
    }
  };

  const copyApiKey = (apiKey: string) => {
    navigator.clipboard.writeText(apiKey);
    setCopiedKey(apiKey);
    showToast('Project API Key copied to clipboard', 'info');
    setTimeout(() => setCopiedKey(null), 2500);
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-2xl glass-panel border border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2.5">
            <Layers className="w-5 h-5 text-blue-400" />
            Projects & Processing Queues
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Configure concurrency limits, retry strategies, and toggle live execution dispatch
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={loadData}
            disabled={loading}
            className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 transition-colors"
            title="Refresh queues & stats"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={() => setIsProjectModalOpen(true)}
            className="px-3.5 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors flex items-center gap-1.5"
          >
            <FolderPlus className="w-3.5 h-3.5" /> New Project
          </button>

          <button
            onClick={() => setIsQueueModalOpen(true)}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all flex items-center gap-1.5"
          >
            <Plus className="w-3.5 h-3.5" /> Create Queue
          </button>
        </div>
      </div>

      {/* Projects Scopes Bar */}
      <div className="p-4 rounded-2xl glass-panel border border-slate-800 space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Project Scopes ({projects.length})</p>
          <span className="text-[11px] text-slate-500">Filter queues by project</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <button
            onClick={() => setSelectedProjectId('')}
            className={`p-3.5 rounded-xl border text-left transition-all ${
              selectedProjectId === ''
                ? 'bg-blue-600/15 text-blue-400 border-blue-500/40 shadow-sm'
                : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:text-slate-200'
            }`}
          >
            <div className="font-bold text-slate-100 text-xs">All Projects Scope</div>
            <p className="text-[11px] text-slate-400 mt-0.5">Showing all queues across platform</p>
          </button>

          {projects.map((p) => (
            <div
              key={p.id}
              onClick={() => setSelectedProjectId(p.id)}
              className={`p-3.5 rounded-xl border cursor-pointer transition-all space-y-2 ${
                selectedProjectId === p.id
                  ? 'bg-blue-600/15 text-blue-400 border-blue-500/40 shadow-sm'
                  : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="font-bold text-slate-100 text-xs truncate">{p.name}</span>
                <span className="text-[10px] text-slate-500 font-mono">
                  {new Date(p.createdAt).toLocaleDateString()}
                </span>
              </div>
              <div className="flex items-center justify-between text-[11px] bg-black/40 px-2 py-1 rounded-lg border border-slate-800/80">
                <span className="font-mono truncate text-slate-400 max-w-[170px]">{p.apiKey}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    copyApiKey(p.apiKey);
                  }}
                  className="text-slate-400 hover:text-slate-200 p-0.5"
                  title="Copy API Key"
                >
                  {copiedKey === p.apiKey ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Queues List */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-200">
            Active Processing Queues ({queues.length})
          </h3>
        </div>

        {loading && queues.length === 0 ? (
          <div className="py-16 text-center text-xs text-slate-400 flex flex-col items-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-400" />
            Loading queues...
          </div>
        ) : queues.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-500 rounded-2xl glass-panel border border-slate-800">
            No queues found in this project. Click <strong>Create Queue</strong> above to create your first queue!
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {queues.map((q) => {
              const stats = queueStats[q.id];
              const runningCount = stats?.jobCounts?.['RUNNING'] || 0;
              const queuedCount = stats?.jobCounts?.['QUEUED'] || 0;
              const completedCount = stats?.jobCounts?.['COMPLETED'] || 0;
              const failedCount = stats?.jobCounts?.['FAILED'] || 0;

              return (
                <div
                  key={q.id}
                  className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-4 hover:border-slate-700/80 transition-all"
                >
                  {/* Card Header */}
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="font-mono font-bold text-slate-100 text-sm">{q.name}</h4>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                            q.isPaused
                              ? 'bg-amber-950 text-amber-300 border border-amber-800'
                              : 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                          }`}
                        >
                          {q.isPaused ? 'PAUSED' : 'ACTIVE'}
                        </span>
                      </div>
                      <p className="text-xs text-slate-400 mt-0.5">Project: {q.projectName}</p>
                    </div>

                    {/* Pause / Resume Button */}
                    <button
                      onClick={() => handleTogglePause(q)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all flex items-center gap-1.5 ${
                        q.isPaused
                          ? 'bg-emerald-950/80 hover:bg-emerald-900 text-emerald-300 border-emerald-800 shadow-md shadow-emerald-900/20'
                          : 'bg-amber-950/80 hover:bg-amber-900 text-amber-300 border-amber-800 shadow-md shadow-amber-900/20'
                      }`}
                    >
                      {q.isPaused ? (
                        <>
                          <Play className="w-3.5 h-3.5 fill-emerald-300" /> Resume Queue
                        </>
                      ) : (
                        <>
                          <Pause className="w-3.5 h-3.5 fill-amber-300" /> Pause Dispatch
                        </>
                      )}
                    </button>
                  </div>

                  {/* Concurrency & Retry Policy Stats */}
                  <div className="grid grid-cols-2 gap-3 p-3 rounded-xl bg-slate-900/60 border border-slate-800/80 text-xs">
                    <div>
                      <span className="text-slate-500 block text-[11px]">Concurrency Limit</span>
                      <span className="font-mono font-bold text-slate-200">
                        {runningCount} / {q.concurrencyLimit} Running
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 block text-[11px]">Retry Policy</span>
                      <span className="text-blue-400 font-medium truncate block">
                        {q.retryPolicyName || 'Default (3 max retries)'}
                      </span>
                    </div>
                  </div>

                  {/* Live Status Chips */}
                  <div className="grid grid-cols-4 gap-2 text-center text-xs">
                    <div className="p-2 rounded-lg bg-blue-950/40 border border-blue-900/60">
                      <span className="text-[10px] text-blue-400 block">Queued</span>
                      <span className="font-mono font-bold text-blue-300">{queuedCount}</span>
                    </div>
                    <div className="p-2 rounded-lg bg-cyan-950/40 border border-cyan-900/60">
                      <span className="text-[10px] text-cyan-400 block">Running</span>
                      <span className="font-mono font-bold text-cyan-300">{runningCount}</span>
                    </div>
                    <div className="p-2 rounded-lg bg-emerald-950/40 border border-emerald-900/60">
                      <span className="text-[10px] text-emerald-400 block">Done</span>
                      <span className="font-mono font-bold text-emerald-300">{completedCount}</span>
                    </div>
                    <div className="p-2 rounded-lg bg-rose-950/40 border border-rose-900/60">
                      <span className="text-[10px] text-rose-400 block">Failed</span>
                      <span className="font-mono font-bold text-rose-300">{failedCount}</span>
                    </div>
                  </div>

                  {/* Action Bar */}
                  <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between text-xs">
                    <span className="text-[10px] font-mono text-slate-500">ID: {q.id.substring(0, 13)}...</span>
                    <button
                      onClick={() => {
                        setTargetQueueForJob({ projectId: q.projectId, queueId: q.id });
                        setIsJobModalOpen(true);
                      }}
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold transition-colors"
                    >
                      <Send className="w-3 h-3 text-blue-400" />
                      Submit Job to Queue
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateProjectModal
        isOpen={isProjectModalOpen}
        onClose={() => setIsProjectModalOpen(false)}
        onSuccess={() => {
          loadData();
          showToast('Project created!', 'success');
        }}
      />

      <CreateQueueModal
        isOpen={isQueueModalOpen}
        onClose={() => setIsQueueModalOpen(false)}
        onSuccess={() => {
          loadData();
          showToast('Queue created!', 'success');
        }}
        defaultProjectId={selectedProjectId}
      />

      <CreateJobModal
        isOpen={isJobModalOpen}
        onClose={() => {
          setIsJobModalOpen(false);
          setTargetQueueForJob(null);
        }}
        onSuccess={() => {
          loadData();
          showToast('Job submitted to queue!', 'success');
        }}
        defaultProjectId={targetQueueForJob?.projectId}
        defaultQueueId={targetQueueForJob?.queueId}
      />
    </div>
  );
}
