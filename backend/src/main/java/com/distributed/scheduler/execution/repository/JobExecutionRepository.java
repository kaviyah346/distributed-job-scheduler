package com.distributed.scheduler.execution.repository;

import com.distributed.scheduler.execution.entity.JobExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {
    List<JobExecutionEntity> findByJobIdOrderByAttemptNumberAsc(UUID jobId);
    List<JobExecutionEntity> findByWorkerIdOrderByStartedAtDesc(String workerId);
}
