package com.distributed.scheduler.worker.service;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.execution.entity.JobExecutionEntity;
import com.distributed.scheduler.execution.service.ExecutionService;
import com.distributed.scheduler.job.dto.JobExecutionContext;
import com.distributed.scheduler.job.dto.JobResult;
import com.distributed.scheduler.job.entity.JobEntity;
import com.distributed.scheduler.job.handler.JobHandler;
import com.distributed.scheduler.job.handler.JobHandlerRegistry;
import com.distributed.scheduler.job.repository.JobRepository;
import com.distributed.scheduler.retry.service.RetryService;
import com.distributed.scheduler.scheduler.service.CronSchedulingService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerPollingService {

    private final JobRepository jobRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final ExecutionService executionService;
    private final WorkerRegistrationService registrationService;
    private final RetryService retryService;
    private final CronSchedulingService cronSchedulingService;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${app.worker.batch-size:5}")
    private int batchSize;

    @Value("${app.worker.thread-pool-size:10}")
    private int threadPoolSize;

    private ExecutorService workerThreadPool;
    private TransactionTemplate transactionTemplate;
    private TransactionTemplate requiresNewTransactionTemplate;

    @PostConstruct
    public void init() {
        this.workerThreadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        log.info("WorkerPollingService initialized with thread pool size: {}", threadPoolSize);
    }

    @Scheduled(fixedDelayString = "${app.worker.poll-interval-ms:1000}")
    public void pollAndClaimJobs() {
        if (!workerEnabled) return;

        int availableCapacity = registrationService.getAvailableCapacity();
        if (availableCapacity <= 0) {
            log.trace("Worker at full capacity ({} running jobs)", threadPoolSize);
            return;
        }

        int fetchCount = Math.min(batchSize, availableCapacity);
        String workerId = registrationService.getWorkerId();
        if (workerId == null) return;

        List<JobEntity> claimedJobs = transactionTemplate.execute(status ->
                jobRepository.claimEligibleJobs(workerId, fetchCount)
        );

        if (claimedJobs != null && !claimedJobs.isEmpty()) {
            log.info("[Worker: {}] Atomically claimed {} eligible jobs for processing", workerId, claimedJobs.size());
            for (JobEntity job : claimedJobs) {
                registrationService.incrementActiveJobs();
                workerThreadPool.submit(() -> processClaimedJob(job.getId(), workerId));
            }
        }
    }

    private void processClaimedJob(UUID jobId, String workerId) {
        JobExecutionEntity execution = null;
        JobExecutionContext context = null;
        int currentAttempt = 1;

        try {
            // 1. Transition job to RUNNING and record execution start in a separate transaction
            final int[] attemptHolder = new int[1];
            execution = requiresNewTransactionTemplate.execute(status -> {
                JobEntity job = jobRepository.findById(jobId).orElseThrow();
                job.setStatus(JobStatus.RUNNING);
                int attempt = job.getCurrentRetryCount() + 1;
                job.setCurrentRetryCount(attempt);
                attemptHolder[0] = attempt;
                jobRepository.save(job);
                return executionService.startExecution(job, workerId, attempt);
            });
            currentAttempt = attemptHolder[0];

            JobEntity currentJob = jobRepository.findById(jobId).orElseThrow();

            // 2. Prepare Context
            context = JobExecutionContext.builder()
                    .jobId(jobId)
                    .executionId(execution.getId())
                    .jobType(currentJob.getJobType())
                    .payload(currentJob.getPayload())
                    .attemptNumber(currentAttempt)
                    .workerId(workerId)
                    .build();

            // 3. Find Handler
            Optional<JobHandler> handlerOpt = handlerRegistry.getHandler(currentJob.getJobType());
            if (handlerOpt.isEmpty()) {
                String errMsg = "No registered JobHandler for type: " + currentJob.getJobType();
                context.logError(errMsg);
                handleExecutionFailure(jobId, execution.getId(), errMsg, null, context);
                return;
            }

            // 4. Execute Handler
            JobHandler handler = handlerOpt.get();
            JobResult result = handler.execute(context);

            if (result != null && result.isSuccess()) {
                handleExecutionSuccess(jobId, execution.getId(), result, context);
            } else {
                String err = (result != null && result.getErrorMessage() != null) 
                        ? result.getErrorMessage() 
                        : "Job execution returned non-success result";
                handleExecutionFailure(jobId, execution.getId(), err, null, context);
            }

        } catch (Exception ex) {
            log.error("Exception during job execution for job {}", jobId, ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            UUID execId = execution != null ? execution.getId() : null;
            handleExecutionFailure(jobId, execId, ex.getMessage(), sw.toString(), context);
        } finally {
            registrationService.decrementActiveJobs();
        }
    }

    private void handleExecutionSuccess(UUID jobId, UUID executionId, JobResult result, JobExecutionContext context) {
        if (context != null && executionId != null) {
            executionService.recordLogs(executionId, context.getInMemoryLogs());
        }

        executionService.completeExecution(executionId, result.getOutputData());

        requiresNewTransactionTemplate.executeWithoutResult(status -> {
            jobRepository.findById(jobId).ifPresent(job -> {
                boolean isCronJob = job.getCronExpression() != null && !job.getCronExpression().isBlank();

                if (isCronJob) {
                    // Cron jobs are re-scheduled instead of being marked COMPLETED
                    cronSchedulingService.scheduleNextRun(job);
                    log.info("Cron job {} completed and re-scheduled for next run.", jobId);
                } else {
                    job.setStatus(JobStatus.COMPLETED);
                    job.setLockedByWorkerId(null);
                    job.setLockedAt(null);
                    job.setUpdatedAt(Instant.now());
                    jobRepository.save(job);
                    log.info("Job {} completed successfully.", jobId);
                }
            });
        });
    }

    private void handleExecutionFailure(UUID jobId, UUID executionId, String errorMessage, String stackTrace, JobExecutionContext context) {
        // 1. Persist logs and mark the execution attempt as FAILED
        if (executionId != null) {
            if (context != null) {
                executionService.recordLogs(executionId, context.getInMemoryLogs());
            }
            executionService.failExecution(executionId, errorMessage, stackTrace);
        }

        // 2. Delegate retry/DLQ decision to RetryService (in a new transaction so it commits independently)
        requiresNewTransactionTemplate.executeWithoutResult(status ->
                retryService.handleFailedJob(jobId, errorMessage, stackTrace)
        );
    }

    @PreDestroy
    public void shutdown() {
        if (workerThreadPool != null) {
            log.info("Shutting down worker thread pool...");
            workerThreadPool.shutdown();
            try {
                if (!workerThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    workerThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerThreadPool.shutdownNow();
            }
        }
    }
}
