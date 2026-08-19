# 📖 Distributed Job Scheduler — User Manual & Operating Guide

Welcome to the **Distributed Job Scheduler Platform**! This guide will walk you through everything you need to know to navigate the console, submit background jobs, configure queues, inspect execution logs, manage retry policies, and recover dead-lettered jobs.

---

## 📑 Table of Contents
1. [Platform Overview & Architecture](#1-platform-overview--architecture)
2. [Quick Access & Authentication](#2-quick-access--authentication)
3. [Console Navigation & Pages](#3-console-navigation--pages)
   - [3.1 System Overview Dashboard](#31-system-overview-dashboard)
   - [3.2 Projects & Queues Hub](#32-projects--queues-hub)
   - [3.3 Jobs Manager & Submissions](#33-jobs-manager--submissions)
   - [3.4 Terminal Execution Logs Console](#34-terminal-execution-logs-console)
   - [3.5 Retry Policies Studio](#35-retry-policies-studio)
   - [3.6 Dead Letter Queue (DLQ) Hub](#36-dead-letter-queue-dlq-hub)
4. [Hands-On Guided Tutorials](#4-hands-on-guided-tutorials)
   - [Tutorial A: Submit Your First Immediate Job](#tutorial-a-submit-your-first-immediate-job)
   - [Tutorial B: Schedule a Recurring Cron Job](#tutorial-b-schedule-a-recurring-cron-job)
   - [Tutorial C: Test Retry Backoff & DLQ Recovery](#tutorial-c-test-retry-backoff--dlq-recovery)
5. [User Roles & Permissions Matrix](#5-user-roles--permissions-matrix)
6. [Troubleshooting & FAQs](#6-troubleshooting--faqs)

---

## 1. Platform Overview & Architecture

The **Distributed Job Scheduler** is a high-throughput, fault-tolerant background execution engine built with:
- **Backend**: Spring Boot 3.3 (Java 21)
- **Database**: PostgreSQL with native `FOR UPDATE SKIP LOCKED` concurrency control
- **Security**: Keycloak OIDC (OAuth2 JWT Bearer tokens)
- **Frontend**: Next.js 14 (React 18 + TypeScript)

### How It Works Under the Hood:
```
[ Client / UI ] ──(Submit Job)──> [ PostgreSQL Database ]
                                           │
                                  (Atomic SKIP LOCKED)
                                           │
                                           ▼
                                [ Active Worker Nodes ]
                                  • Priority ordering
                                  • Concurrency limiting
                                  • Heartbeat monitoring
                                           │
                           ┌───────────────┴───────────────┐
                           ▼                               ▼
                     [ Success ]                      [ Failure ]
                   (COMPLETED status)              (Retry backoff)
                                                           │
                                                  (Max Retries Exceeded)
                                                           │
                                                           ▼
                                                    [ DLQ Storage ]
```

---

## 2. Quick Access & Authentication

- **Frontend URL**: `http://localhost:3000`
- **Backend API**: `http://localhost:8080`
- **Swagger Documentation**: `http://localhost:8080/swagger-ui/index.html`
- **Keycloak Console**: `http://localhost:8180`

### 🔑 User Role Switcher (Top-Right Navbar)
You don't need to log in manually through complex forms. In the top-right corner of the dashboard navbar, you can switch between preloaded Keycloak users instantly:

| Role | Username | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **`ADMIN`** | `admin` | `admin123` | Full access across all projects, queues, jobs, and settings |
| **`DEVELOPER`** | `developer` | `dev123` | Can create projects, create retry policies, and submit jobs |
| **`OPERATOR`** | `operator` | `op123` | Can pause/resume queues, re-queue DLQ jobs, and purge records |

---

## 3. Console Navigation & Pages

### 3.1 System Overview Dashboard (`/`)
The landing page gives you a bird's-eye view of the entire system:
- **KPI Metrics Cards**: Total Jobs, Running Jobs, Queued Jobs, Completed, Retrying Failures, and Dead-Lettered.
- **Active Worker Fleet**: Real-time list of registered worker instances, their host machine, current load concurrency meter (e.g. `2 / 10`), and last heartbeat timestamp.
- **Recent Jobs Activity Stream**: Live feed of the last 10 jobs processed by the cluster.
- **Live Polling Toggle**: Auto-refreshes metrics every 4 seconds so you can watch background jobs execute live.

---

### 3.2 Projects & Queues Hub (`/queues`)
Organize your jobs into isolated projects and custom processing queues:
- **Project Scopes**: Each project generates a unique programmatic API key (`djs_live_...`) with a 1-click copy button.
- **Queue Cards**:
  - Displays assigned concurrency limit (e.g. max 5 concurrent jobs).
  - Shows attached retry policy.
  - Live status chips: Breakdown of `Queued`, `Running`, `Done`, and `Failed` jobs in that specific queue.
- **Pause / Resume Dispatch**:
  - Click **"Pause Dispatch"** to instantly stop workers from picking up new jobs from that queue (currently running jobs will finish safely).
  - Click **"Resume Queue"** to re-enable claiming.
- **Submit Job to Queue**: Submit a job pre-targeted to that queue with a single click.

---

### 3.3 Jobs Manager & Submissions (`/jobs`)
Browse and filter every job in the database:
- **Status Filters**: Filter between `All`, `Queued`, `Running`, `Scheduled`, `Completed`, `Failed`, and `Dead-Lettered`.
- **Project & Queue Selectors**: Narrow down jobs to a specific project scope or queue.
- **Instant Search**: Search by Job Type, Queue Name, or UUID.
- **Job Actions**:
  - **View Logs**: Opens the terminal execution log drawer.
  - **Cancel Job**: Immediately cancels any pending or scheduled job before a worker claims it.

#### 🚀 Submitting a New Job Modal:
Click **"Submit New Job"** to open the creation dialog:
1. **Target Project & Queue**: Select where the job should run.
2. **Job Handler Type**:
   - `SEND_EMAIL`: Dispatches emails with recipient, subject, and body payload.
   - `GENERATE_REPORT`: Generates PDF/CSV reports (`reportType`, `format`).
   - `CLEANUP_DATA`: Prunes obsolete database records (`targetTable`, `retentionDays`).
   - `PROCESS_BATCH`: Processes bulk items in batch chunks (`batchId`, `itemCount`).
3. **Payload Editor**: Edit payload parameters directly in formatted JSON.
4. **Priority Slider (1 - 100)**: Higher priority jobs are claimed first by workers.
5. **Max Retries (0 - 20)**: Number of retry attempts before routing to DLQ.
6. **Advanced Scheduling**:
   - **Delayed Execution**: Set a future delay (e.g., run in 30 seconds).
   - **Recurring Cron Schedule**: Enter UNIX/Spring cron expressions (or choose presets like *"Every minute"*, *"Hourly"*, *"Daily at midnight"*).

---

### 3.4 Terminal Execution Logs Console
Click **"View Logs"** on any job row to open the terminal drawer:
- **Attempt Tabs**: If a job failed and was retried, click between `Attempt #1`, `Attempt #2`, etc.
- **Execution Metadata**: Worker ID, exact execution duration in milliseconds, and start time.
- **Live Terminal Step Logs**: Color-coded log entries with timestamp:
  - `[INFO]`: Operational progress steps.
  - `[WARN]`: Retries and network alerts.
  - `[ERROR]`: Stack traces and exception messages.
- **Result Output Data**: Formatted JSON data returned by the handler upon completion (e.g., generated Report ID, batch item counts, email message IDs).

---

### 3.5 Retry Policies Studio (`/retry-policies`)
Define custom backoff curves for failed jobs:
- **Supported Strategies**:
  1. `FIXED`: Retries at identical intervals (e.g., every 5 seconds).
  2. `LINEAR_BACKOFF`: Increases interval linearly ($Initial \times Attempt$, e.g. 5s $\rightarrow$ 10s $\rightarrow$ 15s).
  3. `EXPONENTIAL_BACKOFF`: Multiplies interval exponentially ($Initial \times Multiplier^{Attempt-1}$, capped at Max Interval).
- **Interactive Sequence Preview**: Shows the exact delay timeline for up to 5 attempts before saving.

---

### 3.6 Dead Letter Queue (DLQ) Hub (`/dlq`)
When a job fails and exhausts all retry attempts, it is automatically routed to the DLQ:
- **Failure Reason & Timestamp**: Read why the job failed (e.g. SMTP Connection Timeout, 503 Service Unavailable).
- **Stack Trace Inspector**: Expand to view the full Java stack trace.
- **1-Click Re-queue**: Click **"Re-queue Job"** $\rightarrow$ Resets the retry counter back to 0 and transitions the job to `QUEUED` so workers pick it up fresh.
- **Purge Record**: Permanently delete unrecoverable jobs.

---

## 4. Hands-On Guided Tutorials

### Tutorial A: Submit Your First Immediate Job
1. Go to **[http://localhost:3000](http://localhost:3000)**.
2. Click **"Submit Job"** in the top banner.
3. Select Job Type **`SEND_EMAIL`**.
4. Set Priority to **`10`**.
5. Click **"Submit Job to Queue"**.
6. Watch the status change on the dashboard from `QUEUED` $\rightarrow$ `RUNNING` $\rightarrow$ `COMPLETED` within 1 second.
7. Click **"View Logs"** to see the simulated SMTP handshake and recipient logs!

---

### Tutorial B: Schedule a Recurring Cron Job
1. Click **"Submit Job"**.
2. Select Job Type **`GENERATE_REPORT`**.
3. Check the **"Recurring Cron Schedule"** box.
4. Select the preset **"Every minute"** (`0 * * * * *`).
5. Click **"Submit Job to Queue"**.
6. The job will enter `QUEUED` status, execute on the minute, mark `COMPLETED`, and immediately re-enqueue itself as `SCHEDULED` for the next minute!

---

### Tutorial C: Test Retry Backoff & DLQ Recovery
1. Go to **Jobs Manager** (`/jobs`) $\rightarrow$ Click **"Submit New Job"**.
2. Select Job Type **`SEND_EMAIL`**.
3. In the JSON payload editor, change the recipient to:
   ```json
   {
     "to": "dlq@company.org",
     "subject": "Failure Test",
     "body": "Testing automated DLQ routing"
   }
   ```
   *(The email handler is configured to simulate failure for addresses starting with `dlq@`)*.
4. Set **Max Retries** to `2`.
5. Click **"Submit Job to Queue"**.
6. Watch the job retry twice with backoff, then transition to **`DEAD_LETTERED`**.
7. Navigate to **Dead Letter Queue** (`/dlq` in the sidebar).
8. Inspect the failure reason, then click **"Re-queue Job"** to bring it back to life!

---

## 5. User Roles & Permissions Matrix

| Feature / Action | `ADMIN` | `DEVELOPER` | `OPERATOR` |
| :--- | :---: | :---: | :---: |
| **View Dashboard & Statistics** | ✅ | ✅ | ✅ |
| **View Jobs & Execution Logs** | ✅ | ✅ | ✅ |
| **Create Projects & View API Keys** | ✅ | ✅ | ❌ |
| **Create Retry Policies** | ✅ | ✅ | ❌ |
| **Submit Background Jobs** | ✅ | ✅ | ❌ |
| **Pause / Resume Queues** | ✅ | ❌ | ✅ |
| **Re-queue / Purge DLQ Jobs** | ✅ | ❌ | ✅ |

*(Switch roles anytime using the navbar dropdown to verify role authorization).*

---

## 6. Troubleshooting & FAQs

### Q: Why do my API requests return 401 Unauthorized?
**A:** Click the **"Connect Keycloak"** button in the top-right navbar or switch users in the dropdown. This fetches a fresh JWT Bearer token and stores it in your browser session.

### Q: Why are jobs staying in `QUEUED` status?
**A:** Check the following:
1. Is the queue **PAUSED**? Go to `/queues` and click **"Resume Queue"**.
2. Is the backend service running? Verify Terminal 1 shows `WorkerPollingService` heartbeat logs.
3. Has the concurrency limit been reached? If 5 jobs are currently `RUNNING` in a queue with limit 5, subsequent jobs wait in `QUEUED` until a worker frees up.

### Q: How do I test the REST APIs via Swagger?
**A:** Open `http://localhost:8080/swagger-ui/index.html` $\rightarrow$ Click the green **Authorize** button $\rightarrow$ Paste your Bearer JWT token $\rightarrow$ Click **Authorize**.
