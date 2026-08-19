package com.distributed.scheduler.retry.repository;

import com.distributed.scheduler.retry.entity.RetryPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RetryPolicyRepository extends JpaRepository<RetryPolicyEntity, UUID> {
    boolean existsByName(String name);
}
