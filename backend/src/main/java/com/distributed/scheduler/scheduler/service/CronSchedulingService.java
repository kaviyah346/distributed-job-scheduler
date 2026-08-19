package com.distributed.scheduler.scheduler.service;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.job.entity.JobEntity;
import com.distributed.scheduler.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

/**
 * Handles cron-based recurring job scheduling.
 *
 * <p>After a cron job completes successfully, this service computes the next
 * trigger time from the job's cron expression and re-enqueues it by resetting
 * status to QUEUED and advancing scheduledAt to the next run time.
 *
 * <p>Uses Spring's built-in {@link CronExpression} parser (already on classpath
 * via spring-context) — no additional dependencies needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CronSchedulingService {

    private final JobRepository jobRepository;

    /**
     * Called by WorkerPollingService immediately after a cron job completes.
     * Computes the next execution time and re-enqueues the job.
     *
     * @param job the just-completed job (must have cronExpression set)
     */
    @Transactional
    public void scheduleNextRun(JobEntity job) {
        String expression = job.getCronExpression();
        if (expression == null || expression.isBlank()) {
            log.warn("[CronScheduler] Job {} has no cron expression — marking COMPLETED", job.getId());
            markCompleted(job.getId());
            return;
        }

        try {
            String normalizedExpression = normalizeCronExpression(expression.trim());
            CronExpression cron = CronExpression.parse(normalizedExpression);
            ZonedDateTime nextTrigger = cron.next(ZonedDateTime.now(ZoneOffset.UTC));

            if (nextTrigger == null) {
                log.warn("[CronScheduler] Cron expression '{}' has no future trigger — job {} will be marked COMPLETED",
                        expression, job.getId());
                markCompleted(job.getId());
                return;
            }

            Instant nextRunAt = nextTrigger.toInstant();

            // Re-load inside this transaction to get a managed entity
            jobRepository.findById(job.getId()).ifPresent(managed -> {
                managed.setStatus(JobStatus.SCHEDULED);
                managed.setScheduledAt(nextRunAt);
                managed.setCurrentRetryCount(0);   // Reset retries for the new run
                managed.setLockedByWorkerId(null);
                managed.setLockedAt(null);
                managed.setUpdatedAt(Instant.now());
                jobRepository.save(managed);
                log.info("[CronScheduler] Job {} (type: {}) re-scheduled. Next run: {} (cron: '{}')",
                        job.getId(), job.getJobType(), nextRunAt, expression);
            });

        } catch (Exception e) {
            log.error("[CronScheduler] Error evaluating cron expression '{}' on job {}: {}",
                    expression, job.getId(), e.getMessage(), e);
            markCompleted(job.getId());
        }
    }

    /**
     * Converts standard 5-field cron (min hour dom month dow) into Spring's 6-field cron (sec min hour dom month dow)
     * if only 5 fields were provided.
     */
    private String normalizeCronExpression(String expression) {
        String[] parts = expression.split("\\s+");
        if (parts.length == 5) {
            // Standard 5-field cron: prepend '0' for seconds
            return "0 " + expression;
        }
        return expression;
    }

    private void markCompleted(java.util.UUID jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.COMPLETED);
            job.setLockedByWorkerId(null);
            job.setLockedAt(null);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
        });
    }
}
