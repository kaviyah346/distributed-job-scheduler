export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ProjectResponse {
  id: string;
  name: string;
  description: string;
  apiKey: string;
  createdAt: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
}

export interface QueueResponse {
  id: string;
  projectId: string;
  projectName: string;
  name: string;
  priority: number;
  concurrencyLimit: number;
  isPaused: boolean;
  retryPolicyId?: string;
  retryPolicyName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateQueueRequest {
  projectId: string;
  name: string;
  priority?: number;
  concurrencyLimit?: number;
  retryPolicyId?: string;
}

export interface QueueStatsResponse {
  queueId: string;
  queueName: string;
  isPaused: boolean;
  concurrencyLimit: number;
  jobCounts: Record<string, number>;
}

export type JobStatus =
  | 'QUEUED'
  | 'SCHEDULED'
  | 'CLAIMED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'DEAD_LETTERED';

export interface JobResponse {
  id: string;
  projectId: string;
  projectName: string;
  queueId: string;
  queueName: string;
  jobType: string;
  payload: Record<string, any>;
  status: JobStatus;
  priority: number;
  currentRetryCount: number;
  maxRetries: number;
  scheduledAt: string;
  cronExpression?: string;
  lockedByWorkerId?: string;
  lockedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateJobRequest {
  projectId: string;
  queueId: string;
  jobType: string;
  payload?: Record<string, any>;
  priority?: number;
  maxRetries?: number;
  scheduledAt?: string;
  cronExpression?: string;
}

export interface JobExecutionResponse {
  id: string;
  jobId: string;
  workerId: string;
  attemptNumber: number;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  startedAt: string;
  completedAt?: string;
  durationMs?: number;
  errorMessage?: string;
  stackTrace?: string;
  resultOutput?: Record<string, any>;
  logs: JobExecutionLogResponse[];
}

export interface JobExecutionLogResponse {
  id: string;
  logLevel: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  message: string;
  timestamp: string;
}

export type RetryStrategy = 'FIXED' | 'LINEAR_BACKOFF' | 'EXPONENTIAL_BACKOFF';

export interface RetryPolicyResponse {
  id: string;
  name: string;
  strategy: RetryStrategy;
  maxRetries: number;
  initialIntervalSeconds: number;
  maxIntervalSeconds: number;
  backoffMultiplier: number;
  createdAt: string;
}

export interface CreateRetryPolicyRequest {
  name: string;
  strategy: RetryStrategy;
  maxRetries?: number;
  initialIntervalSeconds?: number;
  maxIntervalSeconds?: number;
  backoffMultiplier?: number;
}

export interface DlqRecordResponse {
  id: string;
  jobId: string;
  jobType: string;
  queueName: string;
  reason: string;
  lastError: string;
  stackTrace?: string;
  totalAttempts: number;
  deadLetteredAt: string;
}

export interface WorkerResponse {
  id: string;
  hostname: string;
  status: 'ACTIVE' | 'IDLE' | 'BUSY' | 'STOPPED' | 'DEAD';
  currentJobCount: number;
  maxConcurrency: number;
  lastHeartbeatAt: string;
  registeredAt: string;
}

export interface DashboardStatsResponse {
  totalJobs: number;
  queuedJobs: number;
  runningJobs: number;
  completedJobs: number;
  failedJobs: number;
  deadLetteredJobs: number;
  scheduledJobs: number;
  totalProjects: number;
  totalQueues: number;
  activeWorkers: number;
  statusBreakdown: Record<string, number>;
}
