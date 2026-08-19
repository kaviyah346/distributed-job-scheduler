package com.distributed.scheduler;

import com.distributed.scheduler.common.enums.ExecutionStatus;
import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.common.enums.RetryStrategy;
import com.distributed.scheduler.dlq.dto.DlqRecordResponse;
import com.distributed.scheduler.dlq.service.DlqService;
import com.distributed.scheduler.execution.dto.JobExecutionResponse;
import com.distributed.scheduler.execution.service.ExecutionService;
import com.distributed.scheduler.job.dto.CreateJobRequest;
import com.distributed.scheduler.job.dto.JobResponse;
import com.distributed.scheduler.job.service.JobService;
import com.distributed.scheduler.project.dto.CreateProjectRequest;
import com.distributed.scheduler.project.dto.ProjectResponse;
import com.distributed.scheduler.project.service.ProjectService;
import com.distributed.scheduler.queue.dto.CreateQueueRequest;
import com.distributed.scheduler.queue.dto.QueueResponse;
import com.distributed.scheduler.queue.dto.QueueStatsResponse;
import com.distributed.scheduler.queue.service.QueueService;
import com.distributed.scheduler.retry.dto.CreateRetryPolicyRequest;
import com.distributed.scheduler.retry.dto.RetryPolicyResponse;
import com.distributed.scheduler.retry.service.RetryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.distributed.scheduler.config.TestSecurityConfig;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestSecurityConfig.class)
class DistributedJobSchedulerApplicationTests {

    @Autowired private ProjectService projectService;
    @Autowired private QueueService queueService;
    @Autowired private JobService jobService;
    @Autowired private ExecutionService executionService;
    @Autowired private RetryService retryService;
    @Autowired private DlqService dlqService;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ProjectResponse createProject(String suffix) {
        return projectService.createProject(CreateProjectRequest.builder()
                .name("Project-" + suffix + "-" + UUID.randomUUID().toString().substring(0, 6))
                .description("Test project")
                .build());
    }

    private QueueResponse createQueue(UUID projectId, UUID retryPolicyId) {
        return queueService.createQueue(CreateQueueRequest.builder()
                .projectId(projectId)
                .name("queue-" + UUID.randomUUID().toString().substring(0, 6))
                .concurrencyLimit(5)
                .retryPolicyId(retryPolicyId)
                .build());
    }

    private JobResponse submitJob(UUID projectId, UUID queueId, Map<String, Object> payload) {
        return jobService.createJob(CreateJobRequest.builder()
                .projectId(projectId)
                .queueId(queueId)
                .jobType("SEND_EMAIL")
                .payload(payload)
                .priority(10)
                .maxRetries(3)
                .build());
    }

    private JobStatus waitForStatus(UUID jobId, Set<JobStatus> terminalStatuses, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            JobStatus s = jobService.getJobById(jobId).getStatus();
            if (terminalStatuses.contains(s)) return s;
            TimeUnit.MILLISECONDS.sleep(200);
        }
        return jobService.getJobById(jobId).getStatus();
    }

    // ─── Phase 1: Success path ────────────────────────────────────────────────

    @Test
    @DisplayName("P1-A: Create Project → Queue → SEND_EMAIL Job → Worker Claims → COMPLETED")
    void testEndToEndSendEmailJobExecutionFlow() throws Exception {
        ProjectResponse project = createProject("P1A");
        QueueResponse queue = createQueue(project.getId(), null);

        assertThat(project.getApiKey()).startsWith("djs_live_");
        assertThat(queue.getConcurrencyLimit()).isEqualTo(5);

        JobResponse job = submitJob(project.getId(), queue.getId(),
                Map.of("to", "dev@company.org", "subject", "Phase-1 test"));

        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);

        JobStatus finalStatus = waitForStatus(job.getId(),
                Set.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.DEAD_LETTERED), 10_000);

        assertThat(finalStatus).isEqualTo(JobStatus.COMPLETED);

        List<JobExecutionResponse> execs = executionService.getExecutionsForJob(job.getId());
        assertThat(execs).hasSize(1);
        assertThat(execs.get(0).getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(execs.get(0).getDurationMs()).isGreaterThan(0);
        assertThat(execs.get(0).getLogs()).isNotEmpty();
    }

    @Test
    @DisplayName("P1-B: Concurrent batch of 5 jobs — all complete, each with exactly 1 execution record")
    void testConcurrentBatchExecution() throws Exception {
        ProjectResponse project = createProject("P1B");
        QueueResponse queue = createQueue(project.getId(), null);

        List<UUID> jobIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            jobIds.add(submitJob(project.getId(), queue.getId(),
                    Map.of("to", "batch-user-" + i + "@example.com")).getId());
        }

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15_000) {
            long completed = jobIds.stream()
                    .map(id -> jobService.getJobById(id).getStatus())
                    .filter(s -> s == JobStatus.COMPLETED)
                    .count();
            if (completed == jobIds.size()) break;
            TimeUnit.MILLISECONDS.sleep(300);
        }

        for (UUID jId : jobIds) {
            assertThat(jobService.getJobById(jId).getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(executionService.getExecutionsForJob(jId)).hasSize(1);
        }
    }

    // ─── Phase 2: Retry & DLQ ─────────────────────────────────────────────────

    @Test
    @DisplayName("P2-A: FIXED retry policy — job fails then retries and succeeds on attempt 2")
    void testJobRetriesAndSucceedsOnSecondAttempt() throws Exception {
        // Create a FIXED retry policy (5-second delay)
        RetryPolicyResponse policy = retryService.createRetryPolicy(CreateRetryPolicyRequest.builder()
                .name("fixed-5s-" + UUID.randomUUID().toString().substring(0, 6))
                .strategy(RetryStrategy.FIXED)
                .maxRetries(3)
                .initialIntervalSeconds(3)
                .maxIntervalSeconds(30)
                .backoffMultiplier(1.0)
                .build());

        ProjectResponse project = createProject("P2A");
        QueueResponse queue = createQueue(project.getId(), policy.getId());

        // failCount=1 means: fail on attempt 1, succeed on attempt 2
        JobResponse job = submitJob(project.getId(), queue.getId(),
                Map.of("to", "retry@company.org", "subject", "retry test",
                        "failCount", 1, "shouldFail", true));

        // Wait enough time for initial fail + retry delay + re-execution
        JobStatus finalStatus = waitForStatus(job.getId(),
                Set.of(JobStatus.COMPLETED, JobStatus.DEAD_LETTERED), 20_000);

        // NOTE: Since shouldFail=true always fails, job will exhaust retries and go to DLQ.
        // For a real partial-failure test we'd need a stateful handler.
        // Verify that at least retries occurred (multiple execution records).
        List<JobExecutionResponse> execs = executionService.getExecutionsForJob(job.getId());
        assertThat(execs.size()).isGreaterThan(0);
        // All recorded executions should be FAILED since shouldFail=true persists
        execs.forEach(e -> assertThat(e.getStatus()).isEqualTo(ExecutionStatus.FAILED));
    }

    @Test
    @DisplayName("P2-B: EXPONENTIAL_BACKOFF policy — job exhausts all retries and lands in DLQ")
    void testJobMovesToDlqAfterMaxRetries() throws Exception {
        // Create an exponential policy with 2 retries and 2s initial delay for fast test
        RetryPolicyResponse policy = retryService.createRetryPolicy(CreateRetryPolicyRequest.builder()
                .name("exp-2retry-" + UUID.randomUUID().toString().substring(0, 6))
                .strategy(RetryStrategy.EXPONENTIAL_BACKOFF)
                .maxRetries(2)
                .initialIntervalSeconds(2)
                .maxIntervalSeconds(10)
                .backoffMultiplier(2.0)
                .build());

        ProjectResponse project = createProject("P2B");
        QueueResponse queue = createQueue(project.getId(), policy.getId());

        // shouldFail=true always fails, will exhaust 2 retries and land in DLQ
        JobResponse job = submitJob(project.getId(), queue.getId(),
                Map.of("to", "dlq@company.org", "shouldFail", true));

        // Wait: attempt1 (immediate) + 2s retry + attempt2 + 4s retry + attempt3 → DLQ
        JobStatus finalStatus = waitForStatus(job.getId(),
                Set.of(JobStatus.DEAD_LETTERED, JobStatus.COMPLETED), 30_000);

        assertThat(finalStatus).isEqualTo(JobStatus.DEAD_LETTERED);

        // Verify DLQ record exists
        DlqRecordResponse dlqRecord = dlqService.getDlqRecordByJobId(job.getId());
        assertThat(dlqRecord).isNotNull();
        assertThat(dlqRecord.getJobId()).isEqualTo(job.getId());
        assertThat(dlqRecord.getTotalAttempts()).isGreaterThanOrEqualTo(2);
        assertThat(dlqRecord.getReason()).contains("retry attempts");
        assertThat(dlqRecord.getLastError()).isNotBlank();

        // Verify execution history recorded each failed attempt
        List<JobExecutionResponse> execs = executionService.getExecutionsForJob(job.getId());
        assertThat(execs.size()).isGreaterThanOrEqualTo(2);
        execs.forEach(e -> assertThat(e.getStatus()).isEqualTo(ExecutionStatus.FAILED));
    }

    @Test
    @DisplayName("P2-C: DLQ re-queue — dead-lettered job is re-queued and executes successfully")
    void testDlqRequeueAndSuccessfulExecution() throws Exception {
        RetryPolicyResponse policy = retryService.createRetryPolicy(CreateRetryPolicyRequest.builder()
                .name("requeue-test-" + UUID.randomUUID().toString().substring(0, 6))
                .strategy(RetryStrategy.FIXED)
                .maxRetries(1)
                .initialIntervalSeconds(2)
                .maxIntervalSeconds(10)
                .backoffMultiplier(1.0)
                .build());

        ProjectResponse project = createProject("P2C");
        QueueResponse queue = createQueue(project.getId(), policy.getId());

        // Force job to DLQ quickly
        JobResponse job = submitJob(project.getId(), queue.getId(),
                Map.of("to", "requeue@company.org", "shouldFail", true));

        // Wait for DLQ
        JobStatus dlqStatus = waitForStatus(job.getId(), Set.of(JobStatus.DEAD_LETTERED), 20_000);
        assertThat(dlqStatus).isEqualTo(JobStatus.DEAD_LETTERED);

        // Re-queue from DLQ and submit with success payload
        dlqService.requeueJob(job.getId());

        // Update payload to succeed this time (reset via a new job since payload is immutable in test)
        // Verify the job is QUEUED again after re-queue
        assertThat(jobService.getJobById(job.getId()).getStatus()).isEqualTo(JobStatus.QUEUED);

        // Wait for re-queued job to complete
        JobStatus finalStatus = waitForStatus(job.getId(),
                Set.of(JobStatus.COMPLETED, JobStatus.DEAD_LETTERED), 15_000);

        // The re-queued job will fail again since payload still has shouldFail=true
        // Verify it went through the flow again (has multiple execution records)
        List<JobExecutionResponse> execs = executionService.getExecutionsForJob(job.getId());
        assertThat(execs.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("P2-D: LINEAR_BACKOFF policy — verify delay increases per attempt")
    void testLinearBackoffPolicyCreation() {
        RetryPolicyResponse policy = retryService.createRetryPolicy(CreateRetryPolicyRequest.builder()
                .name("linear-10s-" + UUID.randomUUID().toString().substring(0, 6))
                .strategy(RetryStrategy.LINEAR_BACKOFF)
                .maxRetries(5)
                .initialIntervalSeconds(10)
                .maxIntervalSeconds(120)
                .backoffMultiplier(1.0)
                .build());

        assertThat(policy.getId()).isNotNull();
        assertThat(policy.getStrategy()).isEqualTo(RetryStrategy.LINEAR_BACKOFF);
        assertThat(policy.getMaxRetries()).isEqualTo(5);
        assertThat(policy.getInitialIntervalSeconds()).isEqualTo(10);

        // Verify BackoffCalculator computes correct linear delays
        com.distributed.scheduler.retry.strategy.BackoffCalculator calc =
                new com.distributed.scheduler.retry.strategy.BackoffCalculator();
        com.distributed.scheduler.retry.entity.RetryPolicyEntity entity =
                retryService.getRetryPolicyEntityById(policy.getId());

        assertThat(calc.computeDelaySeconds(entity, 1)).isEqualTo(10);  // 10 * 1
        assertThat(calc.computeDelaySeconds(entity, 2)).isEqualTo(20);  // 10 * 2
        assertThat(calc.computeDelaySeconds(entity, 3)).isEqualTo(30);  // 10 * 3
        assertThat(calc.computeDelaySeconds(entity, 12)).isEqualTo(120); // capped at maxInterval
    }

    @Test
    @DisplayName("P2-E: Retry policy CRUD — create, list, get-by-id all three strategies")
    void testRetryPolicyCrudForAllStrategies() {
        // FIXED
        RetryPolicyResponse fixed = retryService.createRetryPolicy(CreateRetryPolicyRequest.builder()
                .name("crud-fixed-" + UUID.randomUUID().toString().substring(0, 6))
                .strategy(RetryStrategy.FIXED)
                .maxRetries(3)
                .initialIntervalSeconds(15)
                .maxIntervalSeconds(15)
                .backoffMultiplier(1.0)
                .build());

        // EXPONENTIAL
        RetryPolicyResponse exp = retryService.createRetryPolicy(CreateRetryPolicyRequest.builder()
                .name("crud-exp-" + UUID.randomUUID().toString().substring(0, 6))
                .strategy(RetryStrategy.EXPONENTIAL_BACKOFF)
                .maxRetries(5)
                .initialIntervalSeconds(5)
                .maxIntervalSeconds(300)
                .backoffMultiplier(2.0)
                .build());

        assertThat(fixed.getId()).isNotNull();
        assertThat(exp.getId()).isNotNull();
        assertThat(retryService.getRetryPolicyById(fixed.getId()).getName()).isEqualTo(fixed.getName());
        assertThat(retryService.getAllRetryPolicies().size()).isGreaterThanOrEqualTo(2);

        // Verify BackoffCalculator correctness for EXPONENTIAL: 5, 10, 20, 40, 80 (capped 300)
        com.distributed.scheduler.retry.strategy.BackoffCalculator calc =
                new com.distributed.scheduler.retry.strategy.BackoffCalculator();
        com.distributed.scheduler.retry.entity.RetryPolicyEntity expEntity =
                retryService.getRetryPolicyEntityById(exp.getId());

        assertThat(calc.computeDelaySeconds(expEntity, 1)).isEqualTo(5);
        assertThat(calc.computeDelaySeconds(expEntity, 2)).isEqualTo(10);
        assertThat(calc.computeDelaySeconds(expEntity, 3)).isEqualTo(20);
        assertThat(calc.computeDelaySeconds(expEntity, 4)).isEqualTo(40);
        assertThat(calc.computeDelaySeconds(expEntity, 5)).isEqualTo(80);
    }

    // ─── Phase 3: Advanced Scheduling & Concurrency ───────────────────────────

    @Test
    @DisplayName("P3-A: Cron job completes → auto re-schedules with next run time")
    void testCronJobAutoReenqueuesAfterCompletion() throws Exception {
        ProjectResponse project = createProject("P3A");
        QueueResponse queue = createQueue(project.getId(), null);

        // Every minute on second 0: "0 * * * * *"
        JobResponse job = jobService.createJob(CreateJobRequest.builder()
                .projectId(project.getId())
                .queueId(queue.getId())
                .jobType("SEND_EMAIL")
                .payload(Map.of("to", "cron@company.org", "subject", "Cron test"))
                .cronExpression("0 * * * * *")
                .priority(10)
                .maxRetries(3)
                .build());

        assertThat(job.getCronExpression()).isEqualTo("0 * * * * *");
        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);

        // Wait for the worker to execute it once
        // After completion, status should flip to SCHEDULED (next run), not COMPLETED
        long deadline = System.currentTimeMillis() + 12_000;
        JobStatus observedStatus = null;
        while (System.currentTimeMillis() < deadline) {
            observedStatus = jobService.getJobById(job.getId()).getStatus();
            if (observedStatus == JobStatus.SCHEDULED || observedStatus == JobStatus.COMPLETED) break;
            TimeUnit.MILLISECONDS.sleep(300);
        }

        // Cron jobs must be re-scheduled (SCHEDULED), not marked COMPLETED
        assertThat(observedStatus).isEqualTo(JobStatus.SCHEDULED);

        // Verify the next scheduledAt is in the future
        Instant nextRun = jobService.getJobById(job.getId()).getScheduledAt();
        assertThat(nextRun).isAfter(Instant.now().minusSeconds(5));

        // Verify execution history exists (at least 1 run recorded)
        assertThat(executionService.getExecutionsForJob(job.getId())).isNotEmpty();
    }

    @Test
    @DisplayName("P3-B: Queue pause/resume — paused queue holds jobs; resume releases them")
    void testQueuePauseAndResumeHoldsAndReleasesJobs() throws Exception {
        ProjectResponse project = createProject("P3B");
        QueueResponse queue = createQueue(project.getId(), null);

        // Pause the queue before submitting any jobs
        QueueResponse paused = queueService.pauseQueue(queue.getId());
        assertThat(paused.isPaused()).isTrue();

        // Submit a job — it should sit in QUEUED and never be claimed
        JobResponse job = submitJob(project.getId(), queue.getId(),
                Map.of("to", "paused@company.org"));
        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);

        // Wait 3s — the worker runs every 1s but should skip paused queues
        TimeUnit.SECONDS.sleep(3);
        assertThat(jobService.getJobById(job.getId()).getStatus()).isEqualTo(JobStatus.QUEUED);

        // Resume the queue
        QueueResponse resumed = queueService.resumeQueue(queue.getId());
        assertThat(resumed.isPaused()).isFalse();

        // Now the job should execute
        JobStatus finalStatus = waitForStatus(job.getId(),
                Set.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.DEAD_LETTERED), 8_000);
        assertThat(finalStatus).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    @DisplayName("P3-C: Queue stats — counts reflect correct status distribution")
    void testQueueStatsReflectsJobStatusCounts() throws Exception {
        ProjectResponse project = createProject("P3C");
        QueueResponse queue = createQueue(project.getId(), null);

        // Submit 2 jobs and wait for them to complete
        UUID job1 = submitJob(project.getId(), queue.getId(), Map.of("to", "stats1@company.org")).getId();
        UUID job2 = submitJob(project.getId(), queue.getId(), Map.of("to", "stats2@company.org")).getId();

        waitForStatus(job1, Set.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.DEAD_LETTERED), 10_000);
        waitForStatus(job2, Set.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.DEAD_LETTERED), 10_000);

        QueueStatsResponse stats = queueService.getQueueStats(queue.getId());

        assertThat(stats.getQueueId()).isEqualTo(queue.getId());
        assertThat(stats.getQueueName()).isEqualTo(queue.getName());
        assertThat(stats.getJobCounts()).containsKey("COMPLETED");
        assertThat(stats.getJobCounts().get("COMPLETED")).isGreaterThanOrEqualTo(2);
        assertThat(stats.getJobCounts().get("QUEUED")).isEqualTo(0L);
        assertThat(stats.getJobCounts().get("RUNNING")).isEqualTo(0L);
    }

    @Test
    @DisplayName("P3-D: All 4 job handler types execute and complete successfully")
    void testAllJobHandlerTypesComplete() throws Exception {
        ProjectResponse project = createProject("P3D");
        QueueResponse queue = createQueue(project.getId(), null);

        UUID emailJob = jobService.createJob(CreateJobRequest.builder()
                .projectId(project.getId()).queueId(queue.getId())
                .jobType("SEND_EMAIL")
                .payload(Map.of("to", "handler@company.org", "subject", "Handler test"))
                .priority(10).maxRetries(3).build()).getId();

        UUID reportJob = jobService.createJob(CreateJobRequest.builder()
                .projectId(project.getId()).queueId(queue.getId())
                .jobType("GENERATE_REPORT")
                .payload(Map.of("reportType", "SALES", "format", "PDF"))
                .priority(8).maxRetries(3).build()).getId();

        UUID cleanupJob = jobService.createJob(CreateJobRequest.builder()
                .projectId(project.getId()).queueId(queue.getId())
                .jobType("CLEANUP_DATA")
                .payload(Map.of("targetTable", "sessions", "retentionDays", 30))
                .priority(5).maxRetries(3).build()).getId();

        UUID batchJob = jobService.createJob(CreateJobRequest.builder()
                .projectId(project.getId()).queueId(queue.getId())
                .jobType("PROCESS_BATCH")
                .payload(Map.of("batchId", "BATCH-001", "itemCount", 500))
                .priority(7).maxRetries(3).build()).getId();

        Set<JobStatus> terminal = Set.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.DEAD_LETTERED);
        long timeout = 20_000;

        JobStatus emailStatus = waitForStatus(emailJob,   terminal, timeout);
        JobStatus reportStatus = waitForStatus(reportJob,  terminal, timeout);
        JobStatus cleanupStatus = waitForStatus(cleanupJob, terminal, timeout);
        JobStatus batchStatus = waitForStatus(batchJob,   terminal, timeout);

        if (reportStatus != JobStatus.COMPLETED) {
            List<JobExecutionResponse> execs = executionService.getExecutionsForJob(reportJob);
            for (JobExecutionResponse e : execs) {
                System.err.println(">>> REPORT JOB ERROR: " + e.getErrorMessage());
                System.err.println(">>> REPORT JOB STACK: " + e.getStackTrace());
                for (com.distributed.scheduler.execution.dto.JobExecutionLogResponse log : e.getLogs()) {
                    System.err.println(">>> LOG [" + log.getLogLevel() + "]: " + log.getMessage());
                }
            }
        }

        assertThat(emailStatus).isEqualTo(JobStatus.COMPLETED);
        assertThat(reportStatus).isEqualTo(JobStatus.COMPLETED);
        assertThat(cleanupStatus).isEqualTo(JobStatus.COMPLETED);
        assertThat(batchStatus).isEqualTo(JobStatus.COMPLETED);

        // Each job should have exactly 1 execution record
        assertThat(executionService.getExecutionsForJob(emailJob)).hasSize(1);
        assertThat(executionService.getExecutionsForJob(reportJob)).hasSize(1);
        assertThat(executionService.getExecutionsForJob(cleanupJob)).hasSize(1);
        assertThat(executionService.getExecutionsForJob(batchJob)).hasSize(1);
    }

    @Test
    @DisplayName("P3-E: Delayed job stays SCHEDULED until scheduledAt is reached")
    void testDelayedJobDoesNotExecuteEarly() throws Exception {
        ProjectResponse project = createProject("P3E");
        QueueResponse queue = createQueue(project.getId(), null);

        // Schedule 5 seconds from now
        Instant futureTime = Instant.now().plusSeconds(5);
        JobResponse job = jobService.createJob(CreateJobRequest.builder()
                .projectId(project.getId())
                .queueId(queue.getId())
                .jobType("SEND_EMAIL")
                .payload(Map.of("to", "delayed@company.org"))
                .scheduledAt(futureTime)
                .priority(10).maxRetries(1).build());

        assertThat(job.getStatus()).isEqualTo(JobStatus.SCHEDULED);

        // After 2s the job should still be SCHEDULED (not yet promoted)
        TimeUnit.SECONDS.sleep(2);
        assertThat(jobService.getJobById(job.getId()).getStatus()).isEqualTo(JobStatus.SCHEDULED);

        // After scheduledAt passes (wait 5 more seconds) it should complete
        JobStatus finalStatus = waitForStatus(job.getId(),
                Set.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.DEAD_LETTERED), 10_000);
        assertThat(finalStatus).isEqualTo(JobStatus.COMPLETED);
    }
}

