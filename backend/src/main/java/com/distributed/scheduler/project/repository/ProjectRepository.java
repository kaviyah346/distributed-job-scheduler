package com.distributed.scheduler.project.repository;

import com.distributed.scheduler.project.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    Optional<ProjectEntity> findByName(String name);
    Optional<ProjectEntity> findByApiKey(String apiKey);
    boolean existsByName(String name);
}
