package com.distributed.scheduler.dlq.service;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.common.exception.ResourceNotFoundException;
import com.distributed.scheduler.dlq.dto.DlqRecordResponse;
import com.distributed.scheduler.dlq.entity.DlqRecordEntity;
import com.distributed.scheduler.dlq.repository.DlqRecordRepository;
import com.distributed.scheduler.job.entity.JobEntity;
import com.distributed.scheduler.job.repository.JobRepository;
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
public class DlqService {

    private final DlqRecordRepository dlqRecordRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public List<DlqRecordResponse> getAllDlqRecords() {
        return dlqRecordRepository.findAll().stream()
                .map(DlqRecordResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DlqRecordResponse getDlqRecordByJobId(UUID jobId) {
        DlqRecordEntity record = dlqRecordRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("No DLQ record found for job: " + jobId));
        return DlqRecordResponse.fromEntity(record);
    }

    /**
     * Re-queues a dead-lettered job back to QUEUED status so it can be picked up again.
     * Resets retry counter to allow fresh attempts.
     */
    @Transactional
    public DlqRecordResponse requeueJob(UUID jobId) {
        DlqRecordEntity record = dlqRecordRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("No DLQ record found for job: " + jobId));

        JobEntity job = record.getJob();
        job.setStatus(JobStatus.QUEUED);
        job.setCurrentRetryCount(0);       // Reset retry counter
        job.setScheduledAt(Instant.now()); // Schedule immediately
        job.setLockedByWorkerId(null);
        job.setLockedAt(null);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        // Remove from DLQ
        dlqRecordRepository.delete(record);

        log.info("[DLQ] Job {} re-queued from DLQ for fresh execution", jobId);
        return DlqRecordResponse.fromEntity(record);
    }

    /**
     * Permanently removes a job from the DLQ (purge).
     */
    @Transactional
    public void purgeJob(UUID jobId) {
        DlqRecordEntity record = dlqRecordRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("No DLQ record found for job: " + jobId));
        dlqRecordRepository.delete(record);
        log.info("[DLQ] Job {} purged from DLQ", jobId);
    }
}
