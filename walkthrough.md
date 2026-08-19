# Distributed Job Scheduler — Phase 1 & 2 Walkthrough

## Phase 1: Core Job Lifecycle ✅

**Status**: Complete & verified

### What was built
| Layer | Classes |
|-------|---------|
| Entities | `ProjectEntity`, `QueueEntity`, `JobEntity`, `JobExecutionEntity`, `JobExecutionLogEntity`, `WorkerEntity` |
| Repositories | JPA + native `FOR UPDATE SKIP LOCKED` claim query in `JobRepository` |
| Job Engine | `JobHandler` interface, `JobHandlerRegistry`, `SendEmailJobHandler` |
| Worker | `WorkerPollingService` (atomic claim + thread pool), `WorkerRegistrationService` |
| Scheduler | `ScheduledJobEligibilityService` (promotes `SCHEDULED → QUEUED`) |
| API | `ProjectController`, `QueueController`, `JobController`, `ExecutionController` |

### Verified flow
```
Create Project → Create Queue → Submit SEND_EMAIL Job
→ QUEUED → Worker claims (FOR UPDATE SKIP LOCKED) → CLAIMED
→ RUNNING → SendEmailJobHandler executes → COMPLETED
→ Execution record created with duration, logs, output
```

---

## Phase 2: Failure, Retry Policies & DLQ ✅

**Status**: Complete & verified — **7/7 tests passing, BUILD SUCCESS**

### New modules added

#### `retry` module
| File | Purpose |
|------|---------|
| `RetryPolicyEntity` | Stores strategy, maxRetries, initial/max intervals, multiplier |
| `RetryPolicyRepository` | JPA repository |
| `BackoffCalculator` | Computes delay seconds for FIXED / LINEAR_BACKOFF / EXPONENTIAL_BACKOFF |
| `RetryService` | Decides retry reschedule vs DLQ after each job failure |
| `RetryPolicyController` | `POST/GET /api/v1/retry-policies` |

#### `dlq` module
| File | Purpose |
|------|---------|
| `DlqRecordEntity` | Stores failed job reference, reason, lastError, stackTrace, attempts |
| `DlqRecordRepository` | JPA repository |
| `DlqService` | List, get, re-queue, purge DLQ records |
| `DlqController` | `GET/POST/DELETE /api/v1/dlq/jobs/{jobId}/...` |

### Retry strategies

| Strategy | Delay Formula | Example (initial=5s, multiplier=2) |
|----------|--------------|-------------------------------------|
| `FIXED` | `initialInterval` | 5s, 5s, 5s, ... |
| `LINEAR_BACKOFF` | `initialInterval × attemptNumber` | 5s, 10s, 15s, ... |
| `EXPONENTIAL_BACKOFF` | `min(initial × multiplier^(n-1), max)` | 5s, 10s, 20s, 40s, ... |

### Retry flow

```
Job RUNNING → Handler throws / returns failure
    ↓
[WorkerPollingService.handleExecutionFailure]
    → ExecutionService.failExecution()   ← records FAILED execution
    → RetryService.handleFailedJob()     ← in REQUIRES_NEW transaction
        ├── currentAttempt < maxRetries?
        │       → reschedule: status=QUEUED, scheduledAt=now+backoffDelay
        │         (held by claim query until scheduledAt is due)
        └── currentAttempt >= maxRetries?
                → status=DEAD_LETTERED
                → DlqRecordEntity saved
```

### DLQ operations

| Endpoint | Action |
|----------|--------|
| `GET /api/v1/dlq` | List all dead-lettered jobs |
| `GET /api/v1/dlq/jobs/{id}` | Get DLQ record for a specific job |
| `POST /api/v1/dlq/jobs/{id}/requeue` | Move job back to QUEUED (retryCount reset to 0) |
| `DELETE /api/v1/dlq/jobs/{id}` | Permanently purge from DLQ |

### Queue ↔ Retry Policy wiring

A retry policy can be attached to a queue at creation time:
```json
POST /api/v1/queues
{
  "projectId": "...",
  "name": "email-queue",
  "retryPolicyId": "<uuid-of-retry-policy>"
}
```
All jobs in that queue inherit the retry policy as their default behavior.

### Test results

```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 36.459 s
```

| Test | Description | Result |
|------|-------------|--------|
| P1-A | E2E SEND_EMAIL → COMPLETED | ✅ |
| P1-B | Concurrent batch of 5 jobs | ✅ |
| P2-A | FIXED retry policy — retries on failure | ✅ |
| P2-B | EXPONENTIAL_BACKOFF — DLQ after maxRetries | ✅ |
| P2-C | DLQ re-queue → fresh execution | ✅ |
| P2-D | LINEAR_BACKOFF delay verification | ✅ |
| P2-E | Retry policy CRUD + BackoffCalculator math | ✅ |

### Live log evidence (from P2-B test)
```
[WorkerPollingService] Atomically claimed 1 eligible jobs for processing
[ExecutionService]     Started execution ... for job xxx (attempt 1)
[SendEmailJobHandler]  Executing SEND_EMAIL for Job: xxx, Recipient: dlq@company.org
[ExecutionService]     Execution xxx failed after 566ms
[RetryService]         Job xxx exhausted all 2 retries. Moving to DEAD_LETTERED.
[DLQ]                  Job xxx dead-lettered after 2 attempts.
```

---

## API Summary (all endpoints)

| Module | Endpoint | Method |
|--------|----------|--------|
| Projects | `/api/v1/projects` | POST, GET |
| Queues | `/api/v1/queues` | POST, GET |
| Jobs | `/api/v1/jobs` | POST, GET, GET by id |
| Executions | `/api/v1/jobs/{id}/executions` | GET |
| Retry Policies | `/api/v1/retry-policies` | POST, GET, GET by id |
| DLQ | `/api/v1/dlq` | GET |
| DLQ Job | `/api/v1/dlq/jobs/{id}` | GET |
| DLQ Requeue | `/api/v1/dlq/jobs/{id}/requeue` | POST |
| DLQ Purge | `/api/v1/dlq/jobs/{id}` | DELETE |

## Swagger UI

Start the app and visit: **http://localhost:8080/swagger-ui/index.html**
