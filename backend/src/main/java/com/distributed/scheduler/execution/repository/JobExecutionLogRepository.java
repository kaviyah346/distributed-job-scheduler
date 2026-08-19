package com.distributed.scheduler.execution.repository;

import com.distributed.scheduler.execution.entity.JobExecutionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLogEntity, UUID> {
    List<JobExecutionLogEntity> findByExecutionIdOrderByTimestampAsc(UUID executionId);
}
