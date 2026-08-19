'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { RetryPolicyResponse, RetryStrategy } from '@/lib/types';
import { CreatePolicyModal } from '@/components/CreatePolicyModal';
import { useToast } from '@/components/Toast';
import {
  RotateCcw,
  Plus,
  RefreshCw,
  Clock,
  Layers,
  Calculator,
  Shield,
} from 'lucide-react';

export default function RetryPoliciesPage() {
  const { showToast } = useToast();
  const [policies, setPolicies] = useState<RetryPolicyResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);

  const fetchPolicies = async () => {
    setLoading(true);
    try {
      const list = await api.getRetryPolicies();
      setPolicies(list);
    } catch (err: any) {
      showToast(err.message || 'Failed to fetch retry policies', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPolicies();
  }, []);

  const calculateDelay = (policy: RetryPolicyResponse, attempt: number): number => {
    switch (policy.strategy) {
      case 'FIXED':
        return policy.initialIntervalSeconds;
      case 'LINEAR_BACKOFF':
        return Math.min(policy.initialIntervalSeconds * attempt, policy.maxIntervalSeconds);
      case 'EXPONENTIAL_BACKOFF':
        return Math.min(
          Math.round(policy.initialIntervalSeconds * Math.pow(policy.backoffMultiplier || 2, attempt - 1)),
          policy.maxIntervalSeconds
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-2xl glass-panel border border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2.5">
            <RotateCcw className="w-5 h-5 text-blue-400" />
            Retry Policies Studio
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Configure exponential, linear, and fixed backoff algorithms for automatic job retry recovery
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={fetchPolicies}
            disabled={loading}
            className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 transition-colors"
            title="Refresh policies"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all flex items-center gap-1.5"
          >
            <Plus className="w-3.5 h-3.5" /> Create Retry Policy
          </button>
        </div>
      </div>

      {/* Policies Grid */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-200">
            Configured Policies ({policies.length})
          </h3>
        </div>

        {loading && policies.length === 0 ? (
          <div className="py-16 text-center text-xs text-slate-400 flex flex-col items-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-400" />
            Loading retry policies...
          </div>
        ) : policies.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-500 rounded-2xl glass-panel border border-slate-800">
            No custom retry policies configured yet. Click <strong>Create Retry Policy</strong> above to add one!
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {policies.map((p) => (
              <div
                key={p.id}
                className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-4 hover:border-slate-700/80 transition-all"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <h4 className="font-mono font-bold text-slate-100 text-sm">{p.name}</h4>
                    <span
                      className={`inline-block mt-1 px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        p.strategy === 'EXPONENTIAL_BACKOFF'
                          ? 'bg-purple-950 text-purple-300 border border-purple-800'
                          : p.strategy === 'LINEAR_BACKOFF'
                          ? 'bg-blue-950 text-blue-300 border border-blue-800'
                          : 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                      }`}
                    >
                      {p.strategy}
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs p-3 rounded-xl bg-slate-900/60 border border-slate-800">
                  <div>
                    <span className="text-slate-500 block text-[10px]">Max Retries</span>
                    <span className="font-mono font-bold text-slate-200">{p.maxRetries} Attempts</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Initial Delay</span>
                    <span className="font-mono font-bold text-slate-200">{p.initialIntervalSeconds}s</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Max Interval Cap</span>
                    <span className="font-mono font-bold text-slate-200">{p.maxIntervalSeconds}s</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Multiplier</span>
                    <span className="font-mono font-bold text-slate-200">{p.backoffMultiplier}x</span>
                  </div>
                </div>

                {/* Projected Sequence Timeline */}
                <div className="space-y-1.5">
                  <span className="text-[10px] uppercase font-bold text-slate-400 flex items-center gap-1">
                    <Calculator className="w-3 h-3 text-blue-400" /> Projected Delay Sequence:
                  </span>
                  <div className="flex items-center gap-1.5 overflow-x-auto py-1">
                    {[1, 2, 3, 4, 5].slice(0, p.maxRetries).map((attempt) => (
                      <div key={attempt} className="px-2 py-1 rounded bg-slate-900 border border-slate-800 text-center shrink-0">
                        <span className="block text-[9px] text-slate-500">#{attempt}</span>
                        <span className="block font-mono font-bold text-blue-400 text-[11px]">
                          +{calculateDelay(p, attempt)}s
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-800/60 text-[10px] text-slate-500 font-mono flex justify-between">
                  <span>ID: {p.id.substring(0, 13)}...</span>
                  <span>{new Date(p.createdAt).toLocaleDateString()}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal */}
      <CreatePolicyModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => {
          fetchPolicies();
          showToast('Retry policy created!', 'success');
        }}
      />
    </div>
  );
}
