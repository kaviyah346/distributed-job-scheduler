package com.distributed.scheduler.queue.repository;

import com.distributed.scheduler.queue.entity.QueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepository extends JpaRepository<QueueEntity, UUID> {
    List<QueueEntity> findByProjectId(UUID projectId);
    Optional<QueueEntity> findByProjectIdAndName(UUID projectId, String name);
    boolean existsByProjectIdAndName(UUID projectId, String name);
}
