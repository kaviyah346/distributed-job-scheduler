package com.distributed.scheduler.scheduler.service;

import com.distributed.scheduler.common.enums.WorkerStatus;
import com.distributed.scheduler.job.entity.JobEntity;
import com.distributed.scheduler.job.repository.JobRepository;
import com.distributed.scheduler.retry.service.RetryService;
import com.distributed.scheduler.worker.entity.WorkerEntity;
import com.distributed.scheduler.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Detects workers whose heartbeat has gone silent (JVM crash, network loss, etc.)
 * and recovers their orphaned jobs by routing them back through RetryService.
 *
 * <p>Algorithm (runs every 30 seconds):
 * <ol>
 *   <li>Find workers where {@code last_heartbeat_at < now - heartbeatTimeoutSeconds}</li>
 *   <li>Mark those workers as {@link WorkerStatus#DEAD}</li>
 *   <li>Find all jobs in RUNNING or CLAIMED state locked by any dead worker</li>
 *   <li>Pass each orphaned job to {@link RetryService#handleFailedJob} so it follows the
 *       normal retry/DLQ path — no special-casing needed</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaleWorkerRecoveryService {

    private final WorkerRepository workerRepository;
    private final JobRepository jobRepository;
    private final RetryService retryService;

    /**
     * Seconds since the last heartbeat before a worker is considered dead.
     * Must be longer than the heartbeat interval (default 5s) to avoid false positives.
     * Default: 60 seconds.
     */
    @Value("${app.worker.heartbeat-timeout-seconds:60}")
    private long heartbeatTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.scheduler.recovery-interval-ms:30000}")
    @Transactional
    public void recoverStaleWorkers() {
        Instant cutoff = Instant.now().minusSeconds(heartbeatTimeoutSeconds);

        List<WorkerEntity> staleWorkers = workerRepository.findByLastHeartbeatAtBefore(cutoff)
                .stream()
                .filter(w -> w.getStatus() != WorkerStatus.STOPPED && w.getStatus() != WorkerStatus.DEAD)
                .collect(Collectors.toList());

        if (staleWorkers.isEmpty()) {
            return;
        }

        List<String> deadWorkerIds = staleWorkers.stream()
                .map(WorkerEntity::getId)
                .collect(Collectors.toList());

        log.warn("[Recovery] Detected {} stale worker(s): {}", deadWorkerIds.size(), deadWorkerIds);

        // Mark workers as DEAD
        for (WorkerEntity worker : staleWorkers) {
            worker.setStatus(WorkerStatus.DEAD);
            workerRepository.save(worker);
            log.warn("[Recovery] Marked worker '{}' (last heartbeat: {}) as DEAD", 
                    worker.getId(), worker.getLastHeartbeatAt());
        }

        // Find and recover orphaned jobs
        List<JobEntity> orphanedJobs = jobRepository.findOrphanedJobs(deadWorkerIds);

        if (orphanedJobs.isEmpty()) {
            log.info("[Recovery] No orphaned jobs found for dead workers: {}", deadWorkerIds);
            return;
        }

        log.warn("[Recovery] Found {} orphaned job(s) from dead workers — routing through retry/DLQ logic", 
                orphanedJobs.size());

        for (JobEntity orphan : orphanedJobs) {
            log.warn("[Recovery] Recovering orphaned job {} (type: {}, status: {}, locked by: {})",
                    orphan.getId(), orphan.getJobType(), orphan.getStatus(), orphan.getLockedByWorkerId());
            try {
                // Delegate to RetryService: it will either reschedule with backoff or dead-letter
                retryService.handleFailedJob(
                        orphan.getId(),
                        "Orphaned by dead worker: " + orphan.getLockedByWorkerId(),
                        null
                );
            } catch (Exception e) {
                log.error("[Recovery] Failed to recover orphaned job {}: {}", orphan.getId(), e.getMessage(), e);
            }
        }
    }
}
