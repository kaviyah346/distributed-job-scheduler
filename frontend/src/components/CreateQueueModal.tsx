'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { ProjectResponse, RetryPolicyResponse, CreateQueueRequest } from '@/lib/types';
import { useToast } from './Toast';
import { X, Layers, Plus } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  defaultProjectId?: string;
}

export const CreateQueueModal: React.FC<Props> = ({
  isOpen,
  onClose,
  onSuccess,
  defaultProjectId,
}) => {
  const { showToast } = useToast();
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [policies, setPolicies] = useState<RetryPolicyResponse[]>([]);
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Form State
  const [projectId, setProjectId] = useState<string>(defaultProjectId || '');
  const [name, setName] = useState<string>('');
  const [priority, setPriority] = useState<number>(0);
  const [concurrencyLimit, setConcurrencyLimit] = useState<number>(5);
  const [retryPolicyId, setRetryPolicyId] = useState<string>('');

  useEffect(() => {
    if (isOpen) {
      loadData();
    }
  }, [isOpen]);

  useEffect(() => {
    if (defaultProjectId) setProjectId(defaultProjectId);
  }, [defaultProjectId]);

  const loadData = async () => {
    try {
      const [pList, polList] = await Promise.all([
        api.getProjects(),
        api.getRetryPolicies(),
      ]);
      setProjects(pList);
      setPolicies(polList);
      if (!projectId && pList.length > 0) {
        setProjectId(pList[0].id);
      }
    } catch (err: any) {
      showToast(err.message || 'Failed to load projects/policies', 'error');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectId || !name.trim()) {
      showToast('Project and queue name are required', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const req: CreateQueueRequest = {
        projectId,
        name: name.trim(),
        priority,
        concurrencyLimit,
        retryPolicyId: retryPolicyId || undefined,
      };

      await api.createQueue(req);
      showToast(`Created queue '${name}' successfully!`, 'success');
      setName('');
      onSuccess();
      onClose();
    } catch (err: any) {
      showToast(err.message || 'Failed to create queue', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-md bg-slate-950 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-900/60">
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              <Layers className="w-4 h-4 text-blue-400" />
              Create Processing Queue
            </h2>
            <p className="text-xs text-slate-400">Configure concurrency limit and retry policy</p>
          </div>
          <button onClick={onClose} className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs">
          <div>
            <label className="block text-slate-300 font-medium mb-1">Parent Project</label>
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
            <label className="block text-slate-300 font-medium mb-1">Queue Name</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. email-queue or high-priority-jobs"
              className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500 font-mono"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-300 font-medium mb-1">Concurrency Limit</label>
              <input
                type="number"
                min="1"
                max="100"
                value={concurrencyLimit}
                onChange={(e) => setConcurrencyLimit(Number(e.target.value))}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200"
              />
              <span className="text-[10px] text-slate-500">Max parallel running jobs</span>
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Queue Priority</label>
              <input
                type="number"
                min="0"
                max="100"
                value={priority}
                onChange={(e) => setPriority(Number(e.target.value))}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200"
              />
              <span className="text-[10px] text-slate-500">Default dispatch priority</span>
            </div>
          </div>

          <div>
            <label className="block text-slate-300 font-medium mb-1">Default Retry Policy (Optional)</label>
            <select
              value={retryPolicyId}
              onChange={(e) => setRetryPolicyId(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500"
            >
              <option value="">No default policy (uses 3 max retries)</option>
              {policies.map((pol) => (
                <option key={pol.id} value={pol.id}>
                  {pol.name} ({pol.strategy}, Max: {pol.maxRetries})
                </option>
              ))}
            </select>
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
              <Plus className="w-3.5 h-3.5" />
              {submitting ? 'Creating...' : 'Create Queue'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
