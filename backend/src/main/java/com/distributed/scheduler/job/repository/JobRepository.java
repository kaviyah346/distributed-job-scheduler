package com.distributed.scheduler.job.repository;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.job.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    @Query(value = """
        WITH eligible_jobs AS (
            SELECT j.id
            FROM jobs j
            JOIN queues q ON j.queue_id = q.id
            WHERE j.status = 'QUEUED'
              AND j.scheduled_at <= CURRENT_TIMESTAMP
              AND q.is_paused = FALSE
              AND (
                  SELECT COUNT(*)
                  FROM jobs r
                  WHERE r.queue_id = q.id AND r.status = 'RUNNING'
              ) < q.concurrency_limit
            ORDER BY j.priority DESC, j.scheduled_at ASC, j.created_at ASC
            FOR UPDATE OF j SKIP LOCKED
            LIMIT :batchSize
        )
        UPDATE jobs
        SET status = 'CLAIMED',
            locked_by_worker_id = :workerId,
            locked_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT id FROM eligible_jobs)
        RETURNING *
    """, nativeQuery = true)
    List<JobEntity> claimEligibleJobs(@Param("workerId") String workerId, @Param("batchSize") int batchSize);

    Page<JobEntity> findByProjectId(UUID projectId, Pageable pageable);
    Page<JobEntity> findByQueueId(UUID queueId, Pageable pageable);
    Page<JobEntity> findByStatus(JobStatus status, Pageable pageable);
    Page<JobEntity> findByProjectIdAndStatus(UUID projectId, JobStatus status, Pageable pageable);

    @Query("SELECT COUNT(j) FROM JobEntity j WHERE j.queue.id = :queueId AND j.status = :status")
    long countByQueueIdAndStatus(@Param("queueId") UUID queueId, @Param("status") JobStatus status);

    long countByStatus(JobStatus status);

    @Query("SELECT j FROM JobEntity j WHERE j.status = 'SCHEDULED' AND j.scheduledAt <= :now")
    List<JobEntity> findDueScheduledJobs(@Param("now") Instant now);

    /** Find RUNNING or CLAIMED jobs whose worker ID is in the given set of dead worker IDs. */
    @Query("SELECT j FROM JobEntity j WHERE j.status IN ('RUNNING', 'CLAIMED') AND j.lockedByWorkerId IN :deadWorkerIds")
    List<JobEntity> findOrphanedJobs(@Param("deadWorkerIds") List<String> deadWorkerIds);
}
