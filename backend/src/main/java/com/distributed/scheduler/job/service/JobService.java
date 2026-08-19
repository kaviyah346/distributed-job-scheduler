package com.distributed.scheduler.job.service;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.common.exception.ResourceNotFoundException;
import com.distributed.scheduler.job.dto.CreateJobRequest;
import com.distributed.scheduler.job.dto.JobResponse;
import com.distributed.scheduler.job.entity.JobEntity;
import com.distributed.scheduler.job.handler.JobHandlerRegistry;
import com.distributed.scheduler.job.repository.JobRepository;
import com.distributed.scheduler.project.entity.ProjectEntity;
import com.distributed.scheduler.project.service.ProjectService;
import com.distributed.scheduler.queue.entity.QueueEntity;
import com.distributed.scheduler.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final ProjectService projectService;
    private final QueueService queueService;
    private final JobHandlerRegistry handlerRegistry;

    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        ProjectEntity project = projectService.getProjectEntityById(request.getProjectId());
        QueueEntity queue = queueService.getQueueEntityById(request.getQueueId());

        if (!queue.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Queue does not belong to the specified project");
        }

        if (!handlerRegistry.hasHandler(request.getJobType())) {
            log.warn("Job created with unhandled jobType '{}' - it will require a registered handler before execution", request.getJobType());
        }

        Instant scheduledTime = request.getScheduledAt() != null ? request.getScheduledAt() : Instant.now();
        JobStatus initialStatus = scheduledTime.isAfter(Instant.now()) ? JobStatus.SCHEDULED : JobStatus.QUEUED;

        JobEntity job = JobEntity.builder()
                .project(project)
                .queue(queue)
                .jobType(request.getJobType().toUpperCase())
                .payload(request.getPayload())
                .status(initialStatus)
                .priority(request.getPriority())
                .maxRetries(request.getMaxRetries())
                .scheduledAt(scheduledTime)
                .cronExpression(request.getCronExpression())
                .build();

        JobEntity saved = jobRepository.save(job);
        log.info("Created job: {} [Type: {}, Status: {}, Priority: {}] in queue: {}", 
                saved.getId(), saved.getJobType(), saved.getStatus(), saved.getPriority(), queue.getName());
        return JobResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID id) {
        JobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));
        return JobResponse.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getJobs(UUID projectId, UUID queueId, JobStatus status, Pageable pageable) {
        Page<JobEntity> page;
        if (projectId != null && status != null) {
            page = jobRepository.findByProjectIdAndStatus(projectId, status, pageable);
        } else if (projectId != null) {
            page = jobRepository.findByProjectId(projectId, pageable);
        } else if (queueId != null) {
            page = jobRepository.findByQueueId(queueId, pageable);
        } else if (status != null) {
            page = jobRepository.findByStatus(status, pageable);
        } else {
            page = jobRepository.findAll(pageable);
        }
        return page.map(JobResponse::fromEntity);
    }

    @Transactional
    public JobResponse cancelJob(UUID id) {
        JobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));

        if (job.getStatus() == JobStatus.RUNNING || job.getStatus() == JobStatus.CLAIMED) {
            throw new IllegalStateException("Cannot cancel a job that is already claimed or running");
        }

        job.setStatus(JobStatus.FAILED);
        JobEntity updated = jobRepository.save(job);
        log.info("Cancelled job: {}", id);
        return JobResponse.fromEntity(updated);
    }
}
