package com.distributed.scheduler.retry.service;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.common.exception.ResourceNotFoundException;
import com.distributed.scheduler.dlq.entity.DlqRecordEntity;
import com.distributed.scheduler.dlq.repository.DlqRecordRepository;
import com.distributed.scheduler.job.entity.JobEntity;
import com.distributed.scheduler.job.repository.JobRepository;
import com.distributed.scheduler.retry.dto.CreateRetryPolicyRequest;
import com.distributed.scheduler.retry.dto.RetryPolicyResponse;
import com.distributed.scheduler.retry.entity.RetryPolicyEntity;
import com.distributed.scheduler.retry.repository.RetryPolicyRepository;
import com.distributed.scheduler.retry.strategy.BackoffCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final RetryPolicyRepository retryPolicyRepository;
    private final JobRepository jobRepository;
    private final DlqRecordRepository dlqRecordRepository;
    private final BackoffCalculator backoffCalculator;

    // ─── Policy Management ────────────────────────────────────────────────────

    @Transactional
    public RetryPolicyResponse createRetryPolicy(CreateRetryPolicyRequest request) {
        if (retryPolicyRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Retry policy '" + request.getName() + "' already exists");
        }
        RetryPolicyEntity entity = RetryPolicyEntity.builder()
                .name(request.getName())
                .strategy(request.getStrategy())
                .maxRetries(request.getMaxRetries())
                .initialIntervalSeconds(request.getInitialIntervalSeconds())
                .maxIntervalSeconds(request.getMaxIntervalSeconds())
                .backoffMultiplier(request.getBackoffMultiplier())
                .build();
        return RetryPolicyResponse.fromEntity(retryPolicyRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public RetryPolicyResponse getRetryPolicyById(UUID id) {
        return RetryPolicyResponse.fromEntity(retryPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retry policy not found: " + id)));
    }

    @Transactional(readOnly = true)
    public RetryPolicyEntity getRetryPolicyEntityById(UUID id) {
        return retryPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retry policy not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<RetryPolicyResponse> getAllRetryPolicies() {
        return retryPolicyRepository.findAll().stream()
                .map(RetryPolicyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ─── Core Retry Logic ─────────────────────────────────────────────────────

    /**
     * Called by the worker after a job execution failure.
     * Decides whether to reschedule (retry with backoff) or move to DLQ.
     *
     * @param jobId        the failed job
     * @param errorMessage the failure message
     * @param stackTrace   optional stack trace
     */
    @Transactional
    public void handleFailedJob(UUID jobId, String errorMessage, String stackTrace) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        int currentAttempt = job.getCurrentRetryCount();  // already incremented when RUNNING
        int maxRetries = job.getMaxRetries();

        // Determine effective retry policy (job-level override or queue default)
        RetryPolicyEntity policy = resolvePolicy(job);

        int effectiveMaxRetries = (policy != null) ? policy.getMaxRetries() : maxRetries;

        if (currentAttempt >= effectiveMaxRetries) {
            // ── Move to DLQ ──────────────────────────────────────────────────
            log.warn("[RetryService] Job {} exhausted all {} retries. Moving to DEAD_LETTERED.", jobId, effectiveMaxRetries);
            job.setStatus(JobStatus.DEAD_LETTERED);
            job.setLockedByWorkerId(null);
            job.setLockedAt(null);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);

            DlqRecordEntity dlqRecord = DlqRecordEntity.builder()
                    .job(job)
                    .reason("Exceeded maximum retry attempts (" + effectiveMaxRetries + ")")
                    .lastError(errorMessage)
                    .stackTrace(stackTrace)
                    .totalAttempts(currentAttempt)
                    .build();
            dlqRecordRepository.save(dlqRecord);
            log.warn("[DLQ] Job {} dead-lettered after {} attempts. Reason: {}", jobId, currentAttempt, errorMessage);
        } else {
            // ── Schedule retry with backoff ──────────────────────────────────
            int retryAttemptNumber = currentAttempt + 1; // next attempt ordinal
            long delaySeconds = (policy != null)
                    ? backoffCalculator.computeDelaySeconds(policy, currentAttempt)
                    : 10L; // default 10s if no policy

            Instant nextRun = Instant.now().plusSeconds(delaySeconds);
            job.setStatus(JobStatus.QUEUED);
            job.setScheduledAt(nextRun);
            job.setLockedByWorkerId(null);
            job.setLockedAt(null);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);

            log.info("[RetryService] Job {} will retry as attempt #{} in {}s (scheduled at {})",
                    jobId, retryAttemptNumber, delaySeconds, nextRun);
        }
    }

    private RetryPolicyEntity resolvePolicy(JobEntity job) {
        // Queue-level default retry policy
        if (job.getQueue() != null && job.getQueue().getRetryPolicy() != null) {
            return job.getQueue().getRetryPolicy();
        }
        return null;
    }
}
