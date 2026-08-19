package com.distributed.scheduler.worker.repository;

import com.distributed.scheduler.common.enums.WorkerStatus;
import com.distributed.scheduler.worker.entity.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<WorkerEntity, String> {
    Optional<WorkerEntity> findById(String id);

    /** Find workers whose heartbeat has not been updated since the given cutoff (candidate dead workers). */
    List<WorkerEntity> findByLastHeartbeatAtBefore(Instant cutoff);

    long countByStatusIn(Collection<WorkerStatus> statuses);
}
