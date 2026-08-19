import {
  ApiResponse,
  ProjectResponse,
  CreateProjectRequest,
  QueueResponse,
  CreateQueueRequest,
  QueueStatsResponse,
  JobResponse,
  CreateJobRequest,
  JobExecutionResponse,
  RetryPolicyResponse,
  CreateRetryPolicyRequest,
  DlqRecordResponse,
  WorkerResponse,
  DashboardStatsResponse,
  JobStatus
} from './types';
import { getStoredToken } from './auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  const body: ApiResponse<T> = await res.json().catch(() => {
    throw new Error(`Server returned status ${res.status}: ${res.statusText}`);
  });

  if (!res.ok || !body.success) {
    throw new Error(body.message || `Request failed with HTTP ${res.status}`);
  }

  return body.data;
}

export const api = {
  // ─── Dashboard ─────────────────────────────────────────────────────────────
  getDashboardStats: () => request<DashboardStatsResponse>('/dashboard/stats'),

  // ─── Workers ───────────────────────────────────────────────────────────────
  getWorkers: () => request<WorkerResponse[]>('/workers'),

  // ─── Projects ──────────────────────────────────────────────────────────────
  getProjects: () => request<ProjectResponse[]>('/projects'),
  getProject: (id: string) => request<ProjectResponse>(`/projects/${id}`),
  createProject: (req: CreateProjectRequest) =>
    request<ProjectResponse>('/projects', {
      method: 'POST',
      body: JSON.stringify(req),
    }),

  // ─── Queues ────────────────────────────────────────────────────────────────
  getQueues: (projectId?: string) => {
    const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
    return request<QueueResponse[]>(`/queues${query}`);
  },
  getQueue: (id: string) => request<QueueResponse>(`/queues/${id}`),
  getQueueStats: (id: string) => request<QueueStatsResponse>(`/queues/${id}/stats`),
  createQueue: (req: CreateQueueRequest) =>
    request<QueueResponse>('/queues', {
      method: 'POST',
      body: JSON.stringify(req),
    }),
  pauseQueue: (id: string) =>
    request<QueueResponse>(`/queues/${id}/pause`, { method: 'PUT' }),
  resumeQueue: (id: string) =>
    request<QueueResponse>(`/queues/${id}/resume`, { method: 'PUT' }),

  // ─── Jobs ──────────────────────────────────────────────────────────────────
  getJobs: async (params?: {
    projectId?: string;
    queueId?: string;
    status?: JobStatus;
    page?: number;
    size?: number;
  }) => {
    const searchParams = new URLSearchParams();
    if (params?.projectId) searchParams.append('projectId', params.projectId);
    if (params?.queueId) searchParams.append('queueId', params.queueId);
    if (params?.status) searchParams.append('status', params.status);
    if (params?.page !== undefined) searchParams.append('page', params.page.toString());
    if (params?.size !== undefined) searchParams.append('size', params.size.toString());

    const query = searchParams.toString() ? `?${searchParams.toString()}` : '';
    return request<{
      content: JobResponse[];
      totalElements: number;
      totalPages: number;
      number: number;
      size: number;
    }>(`/jobs${query}`);
  },
  getJob: (id: string) => request<JobResponse>(`/jobs/${id}`),
  createJob: (req: CreateJobRequest) =>
    request<JobResponse>('/jobs', {
      method: 'POST',
      body: JSON.stringify(req),
    }),
  cancelJob: (id: string) =>
    request<JobResponse>(`/jobs/${id}/cancel`, { method: 'POST' }),

  // ─── Executions & Logs ─────────────────────────────────────────────────────
  getJobExecutions: (jobId: string) =>
    request<JobExecutionResponse[]>(`/jobs/${jobId}/executions`),

  // ─── Retry Policies ────────────────────────────────────────────────────────
  getRetryPolicies: () => request<RetryPolicyResponse[]>('/retry-policies'),
  getRetryPolicy: (id: string) =>
    request<RetryPolicyResponse>(`/retry-policies/${id}`),
  createRetryPolicy: (req: CreateRetryPolicyRequest) =>
    request<RetryPolicyResponse>('/retry-policies', {
      method: 'POST',
      body: JSON.stringify(req),
    }),

  // ─── Dead Letter Queue ─────────────────────────────────────────────────────
  getDlqRecords: () => request<DlqRecordResponse[]>('/dlq'),
  getDlqRecord: (jobId: string) =>
    request<DlqRecordResponse>(`/dlq/jobs/${jobId}`),
  requeueDlqJob: (jobId: string) =>
    request<DlqRecordResponse>(`/dlq/jobs/${jobId}/requeue`, { method: 'POST' }),
  purgeDlqJob: (jobId: string) =>
    request<void>(`/dlq/jobs/${jobId}`, { method: 'DELETE' }),
};
