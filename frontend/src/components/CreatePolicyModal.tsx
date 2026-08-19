'use client';

import React, { useState } from 'react';
import { api } from '@/lib/api';
import { RetryStrategy, CreateRetryPolicyRequest } from '@/lib/types';
import { useToast } from './Toast';
import { X, RotateCcw, Plus, Calculator } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreatePolicyModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const { showToast } = useToast();
  const [name, setName] = useState<string>('');
  const [strategy, setStrategy] = useState<RetryStrategy>('EXPONENTIAL_BACKOFF');
  const [maxRetries, setMaxRetries] = useState<number>(5);
  const [initialIntervalSeconds, setInitialIntervalSeconds] = useState<number>(2);
  const [maxIntervalSeconds, setMaxIntervalSeconds] = useState<number>(60);
  const [backoffMultiplier, setBackoffMultiplier] = useState<number>(2.0);
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Compute live preview delays for 5 attempts
  const calculateDelay = (attempt: number): number => {
    switch (strategy) {
      case 'FIXED':
        return initialIntervalSeconds;
      case 'LINEAR_BACKOFF':
        return Math.min(initialIntervalSeconds * attempt, maxIntervalSeconds);
      case 'EXPONENTIAL_BACKOFF':
        return Math.min(
          Math.round(initialIntervalSeconds * Math.pow(backoffMultiplier, attempt - 1)),
          maxIntervalSeconds
        );
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setSubmitting(true);
    try {
      const req: CreateRetryPolicyRequest = {
        name: name.trim(),
        strategy,
        maxRetries,
        initialIntervalSeconds,
        maxIntervalSeconds,
        backoffMultiplier,
      };

      await api.createRetryPolicy(req);
      showToast(`Created retry policy '${name}' successfully!`, 'success');
      setName('');
      onSuccess();
      onClose();
    } catch (err: any) {
      showToast(err.message || 'Failed to create policy', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-lg bg-slate-950 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-900/60">
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              <RotateCcw className="w-4 h-4 text-blue-400" />
              Configure Retry Policy
            </h2>
            <p className="text-xs text-slate-400">Define backoff calculation rules and attempt limits</p>
          </div>
          <button onClick={onClose} className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs">
          <div>
            <label className="block text-slate-300 font-medium mb-1">Policy Name</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Standard-Exponential-Retry"
              className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500 font-mono"
            />
          </div>

          <div>
            <label className="block text-slate-300 font-medium mb-1">Backoff Strategy</label>
            <div className="grid grid-cols-3 gap-2">
              {(['EXPONENTIAL_BACKOFF', 'LINEAR_BACKOFF', 'FIXED'] as RetryStrategy[]).map((s) => (
                <button
                  type="button"
                  key={s}
                  onClick={() => setStrategy(s)}
                  className={`p-2 rounded-xl border text-center font-semibold transition-all ${
                    strategy === s
                      ? 'bg-blue-600/20 text-blue-400 border-blue-500'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {s.replace('_BACKOFF', '')}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-300 font-medium mb-1">Max Retries</label>
              <input
                type="number"
                min="1"
                max="20"
                value={maxRetries}
                onChange={(e) => setMaxRetries(Number(e.target.value))}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200"
              />
            </div>
            <div>
              <label className="block text-slate-300 font-medium mb-1">Initial Interval (sec)</label>
              <input
                type="number"
                min="1"
                max="3600"
                value={initialIntervalSeconds}
                onChange={(e) => setInitialIntervalSeconds(Number(e.target.value))}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200"
              />
            </div>
          </div>

          {strategy !== 'FIXED' && (
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-slate-300 font-medium mb-1">Max Interval Cap (sec)</label>
                <input
                  type="number"
                  min="1"
                  max="86400"
                  value={maxIntervalSeconds}
                  onChange={(e) => setMaxIntervalSeconds(Number(e.target.value))}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200"
                />
              </div>
              {strategy === 'EXPONENTIAL_BACKOFF' && (
                <div>
                  <label className="block text-slate-300 font-medium mb-1">Multiplier</label>
                  <input
                    type="number"
                    step="0.1"
                    min="1"
                    max="10"
                    value={backoffMultiplier}
                    onChange={(e) => setBackoffMultiplier(Number(e.target.value))}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200"
                  />
                </div>
              )}
            </div>
          )}

          {/* Live Calculation Preview */}
          <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 space-y-2">
            <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <Calculator className="w-3.5 h-3.5 text-blue-400" /> Projected Delay Sequence:
            </p>
            <div className="flex items-center gap-2 overflow-x-auto py-1">
              {[1, 2, 3, 4, 5].slice(0, maxRetries).map((attempt) => (
                <div key={attempt} className="px-2.5 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-center shrink-0">
                  <span className="block text-[10px] text-slate-400">Attempt {attempt}</span>
                  <span className="block font-mono font-bold text-blue-400 text-xs">
                    +{calculateDelay(attempt)}s
                  </span>
                </div>
              ))}
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
              <Plus className="w-3.5 h-3.5" />
              {submitting ? 'Creating...' : 'Save Policy'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
