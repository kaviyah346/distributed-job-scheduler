'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { JobResponse, JobStatus, ProjectResponse, QueueResponse } from '@/lib/types';
import { JobStatusBadge } from '@/components/JobStatusBadge';
import { ExecutionLogsModal } from '@/components/ExecutionLogsModal';
import { CreateJobModal } from '@/components/CreateJobModal';
import { useToast } from '@/components/Toast';
import {
  ListTodo,
  Search,
  Filter,
  Send,
  RefreshCw,
  Terminal,
  Ban,
  ChevronLeft,
  ChevronRight,
  Clock,
  Calendar
} from 'lucide-react';

const STATUS_FILTERS: { label: string; value: JobStatus | 'ALL' }[] = [
  { label: 'All Jobs', value: 'ALL' },
  { label: 'Queued', value: 'QUEUED' },
  { label: 'Running', value: 'RUNNING' },
  { label: 'Scheduled', value: 'SCHEDULED' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Dead-Lettered', value: 'DEAD_LETTERED' },
];

export default function JobsPage() {
  const { showToast } = useToast();
  const [jobs, setJobs] = useState<JobResponse[]>([]);
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [queues, setQueues] = useState<QueueResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // Filters & Pagination
  const [selectedStatus, setSelectedStatus] = useState<JobStatus | 'ALL'>('ALL');
  const [selectedProject, setSelectedProject] = useState<string>('');
  const [selectedQueue, setSelectedQueue] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Modals
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [selectedJobTitle, setSelectedJobTitle] = useState<string | undefined>(undefined);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);

  const loadFilterOptions = async () => {
    try {
      const [pList, qList] = await Promise.all([
        api.getProjects().catch(() => []),
        api.getQueues().catch(() => []),
      ]);
      setProjects(pList);
      setQueues(qList);
    } catch (e) {}
  };

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const params: any = { page, size: 15 };
      if (selectedStatus !== 'ALL') params.status = selectedStatus;
      if (selectedProject) params.projectId = selectedProject;
      if (selectedQueue) params.queueId = selectedQueue;

      const res = await api.getJobs(params);
      setJobs(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      showToast(err.message || 'Failed to fetch jobs', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFilterOptions();
  }, []);

  useEffect(() => {
    fetchJobs();
  }, [selectedStatus, selectedProject, selectedQueue, page]);

  const handleCancelJob = async (jobId: string) => {
    if (!confirm('Are you sure you want to cancel this pending job?')) return;
    try {
      await api.cancelJob(jobId);
      showToast('Job cancelled successfully', 'success');
      fetchJobs();
    } catch (err: any) {
      showToast(err.message || 'Failed to cancel job', 'error');
    }
  };

  // Client-side search filtering by jobType or ID
  const filteredJobs = jobs.filter((j) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      j.jobType.toLowerCase().includes(q) ||
      j.id.toLowerCase().includes(q) ||
      (j.queueName && j.queueName.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-2xl glass-panel border border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2.5">
            <ListTodo className="w-5 h-5 text-blue-400" />
            Jobs Manager
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Browse, inspect execution logs, filter by state, and submit new tasks
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={fetchJobs}
            disabled={loading}
            className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 transition-colors"
            title="Refresh jobs"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all flex items-center gap-1.5"
          >
            <Send className="w-3.5 h-3.5" /> Submit New Job
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="p-4 rounded-2xl glass-panel border border-slate-800 space-y-3.5">
        {/* Status Pill Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          {STATUS_FILTERS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => {
                setSelectedStatus(tab.value);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all ${
                selectedStatus === tab.value
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Search & Project/Queue Selectors */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-slate-800/60 text-xs">
          {/* Keyword Search */}
          <div className="relative">
            <Search className="w-3.5 h-3.5 absolute left-3 top-3 text-slate-500" />
            <input
              type="text"
              placeholder="Search by Job Type, Queue, ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-9 pr-3 py-2 text-slate-200 outline-none focus:border-blue-500"
            />
          </div>

          {/* Project Filter */}
          <select
            value={selectedProject}
            onChange={(e) => {
              setSelectedProject(e.target.value);
              setPage(0);
            }}
            className="bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500 cursor-pointer"
          >
            <option value="">All Projects</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>

          {/* Queue Filter */}
          <select
            value={selectedQueue}
            onChange={(e) => {
              setSelectedQueue(e.target.value);
              setPage(0);
            }}
            className="bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-slate-200 outline-none focus:border-blue-500 cursor-pointer"
          >
            <option value="">All Queues</option>
            {queues.map((q) => (
              <option key={q.id} value={q.id}>{q.name}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Jobs Table */}
      <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <p className="text-xs font-semibold text-slate-400">
            Showing {filteredJobs.length} of {totalElements} jobs
          </p>
        </div>

        {loading && jobs.length === 0 ? (
          <div className="py-16 text-center text-xs text-slate-400 flex flex-col items-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-400" />
            Loading job records...
          </div>
        ) : filteredJobs.length === 0 ? (
          <div className="py-16 text-center text-xs text-slate-500 space-y-2">
            <p>No jobs found matching the selected filters.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 uppercase text-[10px] tracking-wider">
                  <th className="pb-3 font-medium">Job Details</th>
                  <th className="pb-3 font-medium">Target Queue</th>
                  <th className="pb-3 font-medium">Status</th>
                  <th className="pb-3 font-medium">Priority</th>
                  <th className="pb-3 font-medium">Retries</th>
                  <th className="pb-3 font-medium">Scheduled At</th>
                  <th className="pb-3 font-medium">Created</th>
                  <th className="pb-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-medium text-slate-300">
                {filteredJobs.map((job) => (
                  <tr key={job.id} className="hover:bg-slate-900/40 transition-colors">
                    <td className="py-3.5">
                      <div className="font-mono font-bold text-slate-100 flex items-center gap-1.5">
                        {job.jobType}
                        {job.cronExpression && (
                          <span className="px-1.5 py-0.2 text-[9px] rounded bg-indigo-950 text-indigo-300 border border-indigo-800">
                            {job.cronExpression}
                          </span>
                        )}
                      </div>
                      <span className="text-[10px] font-mono text-slate-500">{job.id}</span>
                    </td>
                    <td className="py-3.5 text-slate-300">
                      <div>{job.queueName || 'Default'}</div>
                      <span className="text-[10px] text-slate-500">{job.projectName}</span>
                    </td>
                    <td className="py-3.5">
                      <JobStatusBadge status={job.status} size="sm" />
                    </td>
                    <td className="py-3.5 font-mono text-blue-400 font-bold">{job.priority}</td>
                    <td className="py-3.5 font-mono text-slate-400">
                      {job.currentRetryCount} / {job.maxRetries}
                    </td>
                    <td className="py-3.5 text-slate-400 text-[11px]">
                      {job.scheduledAt ? (
                        <span className="flex items-center gap-1">
                          <Clock className="w-3 h-3 text-slate-500" />
                          {new Date(job.scheduledAt).toLocaleTimeString()}
                        </span>
                      ) : (
                        'Immediate'
                      )}
                    </td>
                    <td className="py-3.5 text-slate-400 text-[11px]">
                      {new Date(job.createdAt).toLocaleTimeString()}
                    </td>
                    <td className="py-3.5 text-right space-x-1.5">
                      <button
                        onClick={() => {
                          setSelectedJobId(job.id);
                          setSelectedJobTitle(job.jobType);
                        }}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-[11px] transition-colors"
                        title="View execution attempts & step logs"
                      >
                        <Terminal className="w-3 h-3 text-blue-400" />
                        Logs
                      </button>

                      {['QUEUED', 'SCHEDULED'].includes(job.status) && (
                        <button
                          onClick={() => handleCancelJob(job.id)}
                          className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-rose-950/60 hover:bg-rose-900/80 text-rose-300 border border-rose-800/60 text-[11px] transition-colors"
                          title="Cancel pending job"
                        >
                          <Ban className="w-3 h-3" />
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-3 border-t border-slate-800 text-xs text-slate-400">
            <span>Page {page + 1} of {totalPages}</span>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 disabled:opacity-40 hover:bg-slate-800"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 disabled:opacity-40 hover:bg-slate-800"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      {selectedJobId && (
        <ExecutionLogsModal
          jobId={selectedJobId}
          jobTitle={selectedJobTitle}
          onClose={() => {
            setSelectedJobId(null);
            setSelectedJobTitle(undefined);
          }}
        />
      )}

      <CreateJobModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => {
          fetchJobs();
          showToast('Job created successfully!', 'success');
        }}
      />
    </div>
  );
}
