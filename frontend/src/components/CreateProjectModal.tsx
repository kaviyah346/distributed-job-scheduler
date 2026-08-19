'use client';

import React, { useState } from 'react';
import { api } from '@/lib/api';
import { ProjectResponse } from '@/lib/types';
import { useToast } from './Toast';
import { X, FolderPlus, Copy, Check } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (newProject: ProjectResponse) => void;
}

export const CreateProjectModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const { showToast } = useToast();
  const [name, setName] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [createdProject, setCreatedProject] = useState<ProjectResponse | null>(null);
  const [copied, setCopied] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setSubmitting(true);
    try {
      const p = await api.createProject({
        name: name.trim(),
        description: description.trim() || undefined,
      });
      setCreatedProject(p);
      showToast(`Created project '${p.name}' successfully!`, 'success');
      onSuccess(p);
    } catch (err: any) {
      showToast(err.message || 'Failed to create project', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const copyApiKey = () => {
    if (createdProject?.apiKey) {
      navigator.clipboard.writeText(createdProject.apiKey);
      setCopied(true);
      showToast('API Key copied to clipboard!', 'info');
      setTimeout(() => setCopied(false), 2500);
    }
  };

  const handleClose = () => {
    setName('');
    setDescription('');
    setCreatedProject(null);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-md bg-slate-950 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-900/60">
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              <FolderPlus className="w-4 h-4 text-blue-400" />
              New Project Scope
            </h2>
            <p className="text-xs text-slate-400">Projects isolate queues, jobs, and generated API keys</p>
          </div>
          <button onClick={handleClose} className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        {createdProject ? (
          <div className="p-5 space-y-4 text-xs">
            <div className="p-4 rounded-xl bg-emerald-950/40 border border-emerald-800/80 space-y-2">
              <p className="font-semibold text-emerald-300">Project '{createdProject.name}' is Ready!</p>
              <p className="text-slate-300 text-[11px]">
                Your unique client API Key has been generated for programmatic access:
              </p>
              <div className="flex items-center gap-2 p-2.5 rounded-lg bg-black/60 border border-emerald-900 font-mono text-emerald-400">
                <span className="truncate flex-1">{createdProject.apiKey}</span>
                <button
                  onClick={copyApiKey}
                  className="p-1 rounded hover:bg-slate-800 text-slate-300"
                  title="Copy API Key"
                >
                  {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              onClick={handleClose}
              className="w-full py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white rounded-xl shadow-lg transition-colors"
            >
              Done
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs">
            <div>
              <label className="block text-slate-300 font-medium mb-1">Project Name</label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Payments-Service or Marketing-App"
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500 font-mono"
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Description (Optional)</label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Short description of this project scope..."
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500"
              />
            </div>

            {/* Footer */}
            <div className="pt-3 border-t border-slate-800 flex items-center justify-end gap-2.5">
              <button
                type="button"
                onClick={handleClose}
                className="px-4 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="px-5 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white rounded-xl shadow-lg shadow-blue-500/20 transition-all flex items-center gap-1.5"
              >
                <FolderPlus className="w-3.5 h-3.5" />
                {submitting ? 'Creating...' : 'Create Project'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
