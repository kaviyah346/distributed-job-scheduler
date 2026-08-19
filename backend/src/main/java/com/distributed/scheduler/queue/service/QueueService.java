package com.distributed.scheduler.queue.service;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.common.exception.ResourceNotFoundException;
import com.distributed.scheduler.job.repository.JobRepository;
import com.distributed.scheduler.project.entity.ProjectEntity;
import com.distributed.scheduler.project.service.ProjectService;
import com.distributed.scheduler.queue.dto.CreateQueueRequest;
import com.distributed.scheduler.queue.dto.QueueResponse;
import com.distributed.scheduler.queue.dto.QueueStatsResponse;
import com.distributed.scheduler.queue.entity.QueueEntity;
import com.distributed.scheduler.queue.repository.QueueRepository;
import com.distributed.scheduler.retry.entity.RetryPolicyEntity;
import com.distributed.scheduler.retry.repository.RetryPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueRepository queueRepository;
    private final ProjectService projectService;
    private final RetryPolicyRepository retryPolicyRepository;
    private final JobRepository jobRepository;

    @Transactional
    public QueueResponse createQueue(CreateQueueRequest request) {
        ProjectEntity project = projectService.getProjectEntityById(request.getProjectId());

        if (queueRepository.existsByProjectIdAndName(request.getProjectId(), request.getName())) {
            throw new IllegalArgumentException("Queue with name '" + request.getName() + "' already exists in project");
        }

        QueueEntity queue = QueueEntity.builder()
                .project(project)
                .name(request.getName())
                .priority(request.getPriority())
                .concurrencyLimit(request.getConcurrencyLimit())
                .isPaused(false)
                .build();

        // Attach retry policy if provided
        if (request.getRetryPolicyId() != null) {
            RetryPolicyEntity policy = retryPolicyRepository.findById(request.getRetryPolicyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Retry policy not found: " + request.getRetryPolicyId()));
            queue.setRetryPolicy(policy);
        }

        QueueEntity saved = queueRepository.save(queue);
        log.info("Created queue '{}' for project '{}' with concurrency limit {}", 
                saved.getName(), project.getName(), saved.getConcurrencyLimit());
        return QueueResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public QueueResponse getQueueById(UUID id) {
        QueueEntity queue = queueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found with ID: " + id));
        return QueueResponse.fromEntity(queue);
    }

    @Transactional(readOnly = true)
    public QueueEntity getQueueEntityById(UUID id) {
        return queueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> getQueuesByProject(UUID projectId) {
        return queueRepository.findByProjectId(projectId).stream()
                .map(QueueResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> getAllQueues() {
        return queueRepository.findAll().stream()
                .map(QueueResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public QueueResponse pauseQueue(UUID id) {
        QueueEntity queue = getQueueEntityById(id);
        queue.setPaused(true);
        log.info("Paused queue: {}", queue.getName());
        return QueueResponse.fromEntity(queueRepository.save(queue));
    }

    @Transactional
    public QueueResponse resumeQueue(UUID id) {
        QueueEntity queue = getQueueEntityById(id);
        queue.setPaused(false);
        log.info("Resumed queue: {}", queue.getName());
        return QueueResponse.fromEntity(queueRepository.save(queue));
    }

    @Transactional(readOnly = true)
    public QueueStatsResponse getQueueStats(UUID id) {
        QueueEntity queue = getQueueEntityById(id);
        Map<String, Long> jobCounts = new java.util.LinkedHashMap<>();
        for (JobStatus status : JobStatus.values()) {
            jobCounts.put(status.name(), jobRepository.countByQueueIdAndStatus(id, status));
        }
        return QueueStatsResponse.builder()
                .queueId(queue.getId())
                .queueName(queue.getName())
                .isPaused(queue.isPaused())
                .concurrencyLimit(queue.getConcurrencyLimit())
                .jobCounts(jobCounts)
                .build();
    }
}
