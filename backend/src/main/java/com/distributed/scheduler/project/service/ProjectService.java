package com.distributed.scheduler.project.service;

import com.distributed.scheduler.common.exception.ResourceNotFoundException;
import com.distributed.scheduler.project.dto.CreateProjectRequest;
import com.distributed.scheduler.project.dto.ProjectResponse;
import com.distributed.scheduler.project.entity.ProjectEntity;
import com.distributed.scheduler.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        if (projectRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Project with name '" + request.getName() + "' already exists");
        }

        String generatedApiKey = generateApiKey();

        ProjectEntity project = ProjectEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .apiKey(generatedApiKey)
                .build();

        ProjectEntity saved = projectRepository.save(project);
        log.info("Created new project: {} with ID: {}", saved.getName(), saved.getId());
        return ProjectResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));
        return ProjectResponse.fromEntity(project);
    }

    @Transactional(readOnly = true)
    public ProjectEntity getProjectEntityById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        return "djs_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
