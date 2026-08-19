package com.distributed.scheduler.dlq.repository;

import com.distributed.scheduler.dlq.entity.DlqRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DlqRecordRepository extends JpaRepository<DlqRecordEntity, UUID> {
    Optional<DlqRecordEntity> findByJobId(UUID jobId);
    boolean existsByJobId(UUID jobId);
}
