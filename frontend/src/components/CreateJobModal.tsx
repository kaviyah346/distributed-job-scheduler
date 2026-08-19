'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { ProjectResponse, QueueResponse, CreateJobRequest } from '@/lib/types';
import { useToast } from './Toast';
import { X, Send, Sliders, Calendar, Clock, AlertCircle } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  defaultProjectId?: string;
  defaultQueueId?: string;
}

const JOB_TEMPLATES: Record<string, Record<string, any>> = {
  SEND_EMAIL: {
    to: "customer@example.com",
    subject: "Monthly Statement Ready",
    body: "Your monthly activity statement is now ready to view."
  },
  GENERATE_REPORT: {
    reportType: "MONTHLY_SALES",
    format: "PDF",
    includeCharts: true
  },
  CLEANUP_DATA: {
    targetTable: "user_sessions",
    retentionDays: 30
  },
  PROCESS_BATCH: {
    batchId: "BATCH-8902",
    itemCount: 250
  }
};

export const CreateJobModal: React.FC<Props> = ({
  isOpen,
  onClose,
  onSuccess,
  defaultProjectId,
  defaultQueueId,
}) => {
  const { showToast } = useToast();
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [queues, setQueues] = useState<QueueResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Form State
  const [projectId, setProjectId] = useState<string>(defaultProjectId || '');
  const [queueId, setQueueId] = useState<string>(defaultQueueId || '');
  const [jobType, setJobType] = useState<string>('SEND_EMAIL');
  const [payloadText, setPayloadText] = useState<string>(JSON.stringify(JOB_TEMPLATES['SEND_EMAIL'], null, 2));
  const [priority, setPriority] = useState<number>(10);
  const [maxRetries, setMaxRetries] = useState<number>(3);
  const [isDelayed, setIsDelayed] = useState<boolean>(false);
  const [delaySeconds, setDelaySeconds] = useState<number>(30);
  const [isCron, setIsCron] = useState<boolean>(false);
  const [cronExpression, setCronExpression] = useState<string>('0 * * * * *');
  const [payloadError, setPayloadError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      loadProjectsAndQueues();
    }
  }, [isOpen]);

  useEffect(() => {
    if (defaultProjectId) setProjectId(defaultProjectId);
    if (defaultQueueId) setQueueId(defaultQueueId);
  }, [defaultProjectId, defaultQueueId]);

  useEffect(() => {
    if (projectId) {
      loadQueuesForProject(projectId);
    }
  }, [projectId]);

  const loadProjectsAndQueues = async () => {
    setLoading(true);
    try {
      const pList = await api.getProjects();
      setProjects(pList);
      if (!projectId && pList.length > 0) {
        setProjectId(pList[0].id);
      }
    } catch (err: any) {
      showToast(err.message || 'Failed to load projects', 'error');
    } finally {
      setLoading(false);
    }
  };

  const loadQueuesForProject = async (pId: string) => {
    try {
      const qList = await api.getQueues(pId);
      setQueues(qList);
      if (qList.length > 0 && !qList.some((q) => q.id === queueId)) {
        setQueueId(qList[0].id);
      }
    } catch (err: any) {
      showToast(err.message || 'Failed to load queues', 'error');
    }
  };

  const handleJobTypeChange = (type: string) => {
    setJobType(type);
    if (JOB_TEMPLATES[type]) {
      setPayloadText(JSON.stringify(JOB_TEMPLATES[type], null, 2));
      setPayloadError(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectId || !queueId) {
      showToast('Please select a project and queue', 'error');
      return;
    }

    let parsedPayload = {};
    if (payloadText.trim()) {
      try {
        parsedPayload = JSON.parse(payloadText);
      } catch (err: any) {
        setPayloadError('Invalid JSON format');
        return;
      }
    }

    setSubmitting(true);
    try {
      const req: CreateJobRequest = {
        projectId,
        queueId,
        jobType,
        payload: parsedPayload,
        priority,
        maxRetries,
      };

      if (isDelayed) {
        const scheduledTime = new Date(Date.now() + delaySeconds * 1000).toISOString();
        req.scheduledAt = scheduledTime;
      }

      if (isCron && cronExpression.trim()) {
        req.cronExpression = cronExpression.trim();
      }

      await api.createJob(req);
      showToast(`Submitted ${jobType} job successfully!`, 'success');
      onSuccess();
      onClose();
    } catch (err: any) {
      showToast(err.message || 'Failed to create job', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-2xl max-h-[90vh] bg-slate-950 border border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-900/60">
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              <Send className="w-4 h-4 text-blue-400" />
              Submit Background Job
            </h2>
            <p className="text-xs text-slate-400">Queue a new job for atomic worker claiming</p>
          </div>
          <button onClick={onClose} className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 overflow-y-auto space-y-4 flex-1 text-xs">
          {/* Target Project & Queue */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-300 font-medium mb-1">Target Project</label>
              <select
                value={projectId}
                onChange={(e) => setProjectId(e.target.value)}
                required
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500"
              >
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Target Queue</label>
              <select
                value={queueId}
                onChange={(e) => setQueueId(e.target.value)}
                required
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500"
              >
                {queues.length === 0 ? (
                  <option value="">No queues in project (create one first)</option>
                ) : (
                  queues.map((q) => (
                    <option key={q.id} value={q.id}>
                      {q.name} ({q.isPaused ? 'PAUSED' : `Concurrency: ${q.concurrencyLimit}`})
                    </option>
                  ))
                )}
              </select>
            </div>
          </div>

          {/* Job Type Selector */}
          <div>
            <label className="block text-slate-300 font-medium mb-1.5">Job Handler Type</label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {['SEND_EMAIL', 'GENERATE_REPORT', 'CLEANUP_DATA', 'PROCESS_BATCH'].map((type) => (
                <button
                  type="button"
                  key={type}
                  onClick={() => handleJobTypeChange(type)}
                  className={`p-2.5 rounded-xl border text-center font-semibold transition-all ${
                    jobType === type
                      ? 'bg-blue-600/20 text-blue-400 border-blue-500 shadow-sm'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200 hover:border-slate-700'
                  }`}
                >
                  {type}
                </button>
              ))}
            </div>
          </div>

          {/* JSON Payload Editor */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-slate-300 font-medium">Job Payload (JSON)</label>
              <span className="text-[11px] text-slate-500">Edit payload data for handler</span>
            </div>
            <textarea
              rows={4}
              value={payloadText}
              onChange={(e) => {
                setPayloadText(e.target.value);
                setPayloadError(null);
              }}
              className={`w-full bg-slate-900 font-mono text-xs text-slate-200 border rounded-xl p-3 outline-none focus:border-blue-500 ${
                payloadError ? 'border-rose-500' : 'border-slate-800'
              }`}
            />
            {payloadError && (
              <p className="text-[11px] text-rose-400 flex items-center gap-1 mt-1">
                <AlertCircle className="w-3.5 h-3.5" /> {payloadError}
              </p>
            )}
          </div>

          {/* Priority & Retries */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800">
            <div>
              <div className="flex justify-between mb-1">
                <label className="text-slate-300 font-medium flex items-center gap-1">
                  <Sliders className="w-3.5 h-3.5 text-blue-400" /> Priority:
                </label>
                <span className="text-blue-400 font-bold">{priority}</span>
              </div>
              <input
                type="range"
                min="1"
                max="100"
                value={priority}
                onChange={(e) => setPriority(Number(e.target.value))}
                className="w-full accent-blue-500 cursor-pointer"
              />
              <span className="text-[10px] text-slate-500">Higher numbers claim first</span>
            </div>

            <div>
              <div className="flex justify-between mb-1">
                <label className="text-slate-300 font-medium">Max Retry Attempts:</label>
                <span className="text-slate-200 font-bold">{maxRetries}</span>
              </div>
              <input
                type="number"
                min="0"
                max="20"
                value={maxRetries}
                onChange={(e) => setMaxRetries(Number(e.target.value))}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-slate-200"
              />
              <span className="text-[10px] text-slate-500">Exhausted jobs route to DLQ</span>
            </div>
          </div>

          {/* Advanced Scheduling: Delayed or Cron */}
          <div className="space-y-3 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800">
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Advanced Scheduling Options</p>
            
            {/* Delay toggle */}
            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 cursor-pointer text-slate-200">
                <input
                  type="checkbox"
                  checked={isDelayed}
                  onChange={(e) => {
                    setIsDelayed(e.target.checked);
                    if (e.target.checked) setIsCron(false);
                  }}
                  className="rounded border-slate-700 bg-slate-900 text-blue-600 focus:ring-0"
                />
                <Clock className="w-3.5 h-3.5 text-amber-400" />
                Delayed Execution
              </label>

              {isDelayed && (
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min="5"
                    max="3600"
                    value={delaySeconds}
                    onChange={(e) => setDelaySeconds(Number(e.target.value))}
                    className="w-20 bg-slate-900 border border-slate-700 rounded-lg px-2 py-1 text-slate-200 text-right"
                  />
                  <span className="text-slate-400 text-xs">seconds from now</span>
                </div>
              )}
            </div>

            {/* Cron toggle */}
            <div className="flex flex-col gap-2 pt-2 border-t border-slate-800">
              <label className="flex items-center gap-2 cursor-pointer text-slate-200">
                <input
                  type="checkbox"
                  checked={isCron}
                  onChange={(e) => {
                    setIsCron(e.target.checked);
                    if (e.target.checked) setIsDelayed(false);
                  }}
                  className="rounded border-slate-700 bg-slate-900 text-blue-600 focus:ring-0"
                />
                <Calendar className="w-3.5 h-3.5 text-indigo-400" />
                Recurring Cron Schedule
              </label>

              {isCron && (
                <div className="space-y-2 mt-1">
                  <input
                    type="text"
                    value={cronExpression}
                    onChange={(e) => setCronExpression(e.target.value)}
                    placeholder="e.g. 0 * * * * * or */5 * * * * *"
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 font-mono text-slate-200 text-xs outline-none focus:border-indigo-500"
                  />
                  <div className="flex flex-wrap gap-1.5">
                    {[
                      { label: 'Every minute', expr: '0 * * * * *' },
                      { label: 'Every 5 mins', expr: '0 */5 * * * *' },
                      { label: 'Hourly', expr: '0 0 * * * *' },
                      { label: 'Daily at midnight', expr: '0 0 0 * * *' },
                    ].map((preset) => (
                      <button
                        type="button"
                        key={preset.label}
                        onClick={() => setCronExpression(preset.expr)}
                        className="px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-slate-200 text-[11px]"
                      >
                        {preset.label}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="pt-3 border-t border-slate-800 flex items-center justify-end gap-2.5">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 rounded-xl transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white rounded-xl shadow-lg shadow-blue-500/20 transition-all flex items-center gap-1.5"
            >
              <Send className="w-3.5 h-3.5" />
              {submitting ? 'Submitting Job...' : 'Submit Job to Queue'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
