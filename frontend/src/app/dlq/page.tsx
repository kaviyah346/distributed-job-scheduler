'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { DlqRecordResponse } from '@/lib/types';
import { useToast } from '@/components/Toast';
import {
  AlertOctagon,
  RotateCcw,
  Trash2,
  RefreshCw,
  Clock,
  Layers,
  ChevronDown,
  ChevronUp,
  ShieldAlert,
} from 'lucide-react';

export default function DlqPage() {
  const { showToast } = useToast();
  const [dlqRecords, setDlqRecords] = useState<DlqRecordResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [expandedJobId, setExpandedJobId] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<Record<string, boolean>>({});

  const fetchDlq = async () => {
    setLoading(true);
    try {
      const list = await api.getDlqRecords();
      setDlqRecords(list);
    } catch (err: any) {
      showToast(err.message || 'Failed to fetch DLQ records', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDlq();
  }, []);

  const handleRequeue = async (jobId: string) => {
    setActionLoading((prev) => ({ ...prev, [jobId]: true }));
    try {
      await api.requeueDlqJob(jobId);
      showToast('Job re-queued successfully! Retries reset to 0.', 'success');
      fetchDlq();
    } catch (err: any) {
      showToast(err.message || 'Failed to re-queue job', 'error');
    } finally {
      setActionLoading((prev) => ({ ...prev, [jobId]: false }));
    }
  };

  const handlePurge = async (jobId: string) => {
    if (!confirm('Are you sure you want to permanently purge this dead-lettered job?')) return;
    setActionLoading((prev) => ({ ...prev, [jobId]: true }));
    try {
      await api.purgeDlqJob(jobId);
      showToast('Job purged from DLQ', 'info');
      fetchDlq();
    } catch (err: any) {
      showToast(err.message || 'Failed to purge job', 'error');
    } finally {
      setActionLoading((prev) => ({ ...prev, [jobId]: false }));
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-2xl glass-panel border border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2.5">
            <AlertOctagon className="w-5 h-5 text-red-400" />
            Dead Letter Queue (DLQ) Hub
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Review exhausted execution failures, inspect stack traces, and trigger instant re-queueing
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={fetchDlq}
            disabled={loading}
            className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 transition-colors"
            title="Refresh DLQ"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* DLQ Records List */}
      <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-red-400" />
            Exhausted Job Failures ({dlqRecords.length})
          </h3>
          <span className="text-[11px] text-slate-400">Exhausted retry threshold</span>
        </div>

        {loading && dlqRecords.length === 0 ? (
          <div className="py-16 text-center text-xs text-slate-400 flex flex-col items-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-400" />
            Loading DLQ records...
          </div>
        ) : dlqRecords.length === 0 ? (
          <div className="p-12 text-center text-xs text-emerald-400/80 rounded-2xl bg-emerald-950/20 border border-emerald-800/40">
            ✨ DLQ is completely clean! No jobs have exhausted their retries.
          </div>
        ) : (
          <div className="space-y-3">
            {dlqRecords.map((record) => {
              const isExpanded = expandedJobId === record.jobId;
              const isBusy = actionLoading[record.jobId];

              return (
                <div
                  key={record.id}
                  className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 text-xs space-y-3 hover:border-slate-700/80 transition-all"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-slate-100 text-sm">
                          {record.jobType}
                        </span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-950 text-red-400 border border-red-800">
                          {record.totalAttempts} Attempts Failed
                        </span>
                        <span className="text-[11px] text-slate-400 font-mono">
                          Queue: {record.queueName}
                        </span>
                      </div>
                      <p className="text-[11px] font-mono text-slate-500">Job ID: {record.jobId}</p>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleRequeue(record.jobId)}
                        disabled={isBusy}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-950/80 hover:bg-emerald-900 text-emerald-300 border border-emerald-800 text-xs font-semibold shadow-md shadow-emerald-900/20 transition-all disabled:opacity-50"
                      >
                        <RotateCcw className={`w-3.5 h-3.5 ${isBusy ? 'animate-spin' : ''}`} />
                        Re-queue Job
                      </button>

                      <button
                        onClick={() => handlePurge(record.jobId)}
                        disabled={isBusy}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-rose-950/60 hover:bg-rose-900 text-rose-300 border border-rose-800/60 text-xs font-semibold transition-all disabled:opacity-50"
                        title="Purge record"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Purge
                      </button>
                    </div>
                  </div>

                  {/* Error Reason Banner */}
                  <div className="p-3 rounded-lg bg-rose-950/30 border border-rose-800/60 space-y-1 text-slate-300">
                    <div className="flex items-center justify-between text-[11px] text-rose-400 font-semibold">
                      <span>Failure Reason: {record.reason}</span>
                      <span>Dead-Lettered: {new Date(record.deadLetteredAt).toLocaleTimeString()}</span>
                    </div>
                    <p className="font-mono text-xs text-rose-200">{record.lastError}</p>
                  </div>

                  {/* Stack Trace Toggle */}
                  {record.stackTrace && (
                    <div>
                      <button
                        onClick={() => setExpandedJobId(isExpanded ? null : record.jobId)}
                        className="text-[11px] text-slate-400 hover:text-slate-200 flex items-center gap-1 transition-colors"
                      >
                        {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                        {isExpanded ? 'Hide Full Stack Trace' : 'Show Full Stack Trace'}
                      </button>

                      {isExpanded && (
                        <pre className="mt-2 p-3 rounded-lg bg-black/60 text-rose-300 font-mono text-[11px] overflow-x-auto whitespace-pre-wrap border border-slate-800">
                          {record.stackTrace}
                        </pre>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
