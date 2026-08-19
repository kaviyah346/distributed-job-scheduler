'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { JobExecutionResponse } from '@/lib/types';
import { X, RefreshCw, Terminal, CheckCircle2, XCircle, Clock, Server, Layers } from 'lucide-react';

interface Props {
  jobId: string | null;
  jobTitle?: string;
  onClose: () => void;
}

export const ExecutionLogsModal: React.FC<Props> = ({ jobId, jobTitle, onClose }) => {
  const [executions, setExecutions] = useState<JobExecutionResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedAttempt, setSelectedAttempt] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);

  const fetchLogs = async () => {
    if (!jobId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.getJobExecutions(jobId);
      setExecutions(data);
      if (data.length > 0) {
        setSelectedAttempt(data.length - 1); // Select latest attempt
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load execution logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (jobId) {
      fetchLogs();
    }
  }, [jobId]);

  if (!jobId) return null;

  const currentExec = executions[selectedAttempt];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-4xl max-h-[90vh] bg-slate-950 border border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-900/60">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20">
              <Terminal className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
                Execution History & Logs
              </h2>
              <p className="text-xs text-slate-400 font-mono">Job ID: {jobId} {jobTitle ? `• ${jobTitle}` : ''}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={fetchLogs}
              disabled={loading}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
              title="Refresh logs"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Attempt Tabs */}
        {executions.length > 1 && (
          <div className="flex items-center gap-2 px-5 py-2.5 bg-slate-900/40 border-b border-slate-800 overflow-x-auto">
            <span className="text-xs font-semibold text-slate-400 mr-2 flex items-center gap-1">
              <Layers className="w-3.5 h-3.5" /> Attempts:
            </span>
            {executions.map((exec, idx) => (
              <button
                key={exec.id}
                onClick={() => setSelectedAttempt(idx)}
                className={`px-3 py-1 text-xs rounded-lg font-medium transition-colors flex items-center gap-1.5 ${
                  selectedAttempt === idx
                    ? 'bg-blue-600 text-white font-semibold'
                    : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                }`}
              >
                {exec.status === 'COMPLETED' && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />}
                {exec.status === 'FAILED' && <XCircle className="w-3.5 h-3.5 text-rose-400" />}
                {exec.status === 'RUNNING' && <Clock className="w-3.5 h-3.5 text-cyan-400 animate-spin" />}
                Attempt #{exec.attemptNumber}
              </button>
            ))}
          </div>
        )}

        {/* Content Area */}
        <div className="p-5 overflow-y-auto space-y-4 flex-1">
          {loading && executions.length === 0 ? (
            <div className="py-16 text-center text-slate-400 text-sm flex flex-col items-center gap-2">
              <RefreshCw className="w-6 h-6 animate-spin text-blue-400" />
              Loading execution logs...
            </div>
          ) : error ? (
            <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800 text-rose-300 text-sm">
              {error}
            </div>
          ) : !currentExec ? (
            <div className="py-16 text-center text-slate-500 text-sm">
              No execution records recorded for this job yet. The job is queued or pending dispatch.
            </div>
          ) : (
            <>
              {/* Execution Summary Bar */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs">
                <div>
                  <span className="text-slate-500 block">Status</span>
                  <span className={`font-semibold ${currentExec.status === 'COMPLETED' ? 'text-emerald-400' : currentExec.status === 'FAILED' ? 'text-rose-400' : 'text-cyan-400'}`}>
                    {currentExec.status}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 block">Worker Node</span>
                  <span className="text-slate-200 font-mono flex items-center gap-1">
                    <Server className="w-3.5 h-3.5 text-slate-400" />
                    {currentExec.workerId || 'N/A'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 block">Duration</span>
                  <span className="text-slate-200 font-mono">
                    {currentExec.durationMs !== undefined && currentExec.durationMs !== null ? `${currentExec.durationMs} ms` : 'Running...'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 block">Started At</span>
                  <span className="text-slate-200 font-mono">
                    {new Date(currentExec.startedAt).toLocaleTimeString()}
                  </span>
                </div>
              </div>

              {/* Error Message & Stack Trace if failed */}
              {currentExec.errorMessage && (
                <div className="p-4 rounded-xl bg-rose-950/30 border border-rose-800/80 space-y-2">
                  <p className="text-xs font-bold uppercase tracking-wider text-rose-400">Execution Error:</p>
                  <p className="text-sm font-medium text-rose-200">{currentExec.errorMessage}</p>
                  {currentExec.stackTrace && (
                    <details className="mt-2 text-xs">
                      <summary className="text-rose-400 cursor-pointer hover:underline">View Stack Trace</summary>
                      <pre className="mt-2 p-3 rounded-lg bg-black/60 text-rose-300 font-mono text-[11px] overflow-x-auto whitespace-pre-wrap">
                        {currentExec.stackTrace}
                      </pre>
                    </details>
                  )}
                </div>
              )}

              {/* Terminal Log Stream */}
              <div className="space-y-2">
                <p className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Terminal className="w-3.5 h-3.5 text-blue-400" /> Execution Step Logs ({currentExec.logs?.length || 0})
                </p>
                <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 font-mono text-xs text-slate-300 space-y-1.5 max-h-64 overflow-y-auto">
                  {currentExec.logs && currentExec.logs.length > 0 ? (
                    currentExec.logs.map((log, i) => (
                      <div key={log.id || i} className="flex items-start gap-2.5 leading-relaxed">
                        <span className="text-slate-500 text-[11px] shrink-0">
                          {new Date(log.timestamp).toISOString().substring(11, 23)}
                        </span>
                        <span
                          className={`px-1.5 py-0.2 rounded text-[10px] font-bold shrink-0 ${
                            log.logLevel === 'INFO'
                              ? 'bg-blue-950 text-blue-400 border border-blue-800'
                              : log.logLevel === 'WARN'
                              ? 'bg-amber-950 text-amber-400 border border-amber-800'
                              : log.logLevel === 'ERROR'
                              ? 'bg-rose-950 text-rose-400 border border-rose-800'
                              : 'bg-slate-800 text-slate-400'
                          }`}
                        >
                          {log.logLevel}
                        </span>
                        <span className="text-slate-200 flex-1">{log.message}</span>
                      </div>
                    ))
                  ) : (
                    <p className="text-slate-500 italic">No structured log entries recorded for this attempt.</p>
                  )}
                </div>
              </div>

              {/* Result Output JSON if present */}
              {currentExec.resultOutput && Object.keys(currentExec.resultOutput).length > 0 && (
                <div className="space-y-2">
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Result Output Data:</p>
                  <pre className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 font-mono text-xs text-emerald-300 overflow-x-auto">
                    {JSON.stringify(currentExec.resultOutput, null, 2)}
                  </pre>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-800 bg-slate-900/60 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold text-slate-200 bg-slate-800 hover:bg-slate-700 rounded-xl transition-colors"
          >
            Close Viewer
          </button>
        </div>
      </div>
    </div>
  );
};
