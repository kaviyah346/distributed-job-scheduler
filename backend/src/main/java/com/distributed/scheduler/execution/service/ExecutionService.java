package com.distributed.scheduler.execution.service;

import com.distributed.scheduler.common.enums.ExecutionStatus;
import com.distributed.scheduler.common.enums.LogLevel;
import com.distributed.scheduler.common.exception.ResourceNotFoundException;
import com.distributed.scheduler.execution.dto.JobExecutionResponse;
import com.distributed.scheduler.execution.entity.JobExecutionEntity;
import com.distributed.scheduler.execution.entity.JobExecutionLogEntity;
import com.distributed.scheduler.execution.repository.JobExecutionLogRepository;
import com.distributed.scheduler.execution.repository.JobExecutionRepository;
import com.distributed.scheduler.job.entity.JobEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionService {

    private final JobExecutionRepository executionRepository;
    private final JobExecutionLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobExecutionEntity startExecution(JobEntity job, String workerId, int attemptNumber) {
        JobExecutionEntity execution = JobExecutionEntity.builder()
                .job(job)
                .workerId(workerId)
                .attemptNumber(attemptNumber)
                .status(ExecutionStatus.RUNNING)
                .startedAt(Instant.now())
                .build();

        JobExecutionEntity saved = executionRepository.save(execution);
        log.info("Started execution {} for job {} (attempt {}) on worker {}", 
                saved.getId(), job.getId(), attemptNumber, workerId);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLog(JobExecutionEntity execution, LogLevel level, String message) {
        JobExecutionLogEntity logEntity = JobExecutionLogEntity.builder()
                .execution(execution)
                .logLevel(level)
                .message(message)
                .build();
        logRepository.save(logEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogs(UUID executionId, List<com.distributed.scheduler.job.dto.JobExecutionContext.ExecutionLogEntry> logEntries) {
        if (executionId == null || logEntries == null || logEntries.isEmpty()) return;
        JobExecutionEntity executionRef = executionRepository.getReferenceById(executionId);
        List<JobExecutionLogEntity> entities = logEntries.stream()
                .map(e -> JobExecutionLogEntity.builder()
                        .execution(executionRef)
                        .logLevel(e.getLevel())
                        .message(e.getMessage())
                        .build())
                .collect(Collectors.toList());
        logRepository.saveAll(entities);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeExecution(UUID executionId, Map<String, Object> outputData) {
        JobExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with ID: " + executionId));

        Instant completedAt = Instant.now();
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setCompletedAt(completedAt);
        execution.setDurationMs(Duration.between(execution.getStartedAt(), completedAt).toMillis());
        execution.setResultOutput(outputData);

        executionRepository.save(execution);
        log.info("Completed execution {} in {} ms", executionId, execution.getDurationMs());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failExecution(UUID executionId, String errorMessage, String stackTrace) {
        JobExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with ID: " + executionId));

        Instant completedAt = Instant.now();
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setCompletedAt(completedAt);
        execution.setDurationMs(Duration.between(execution.getStartedAt(), completedAt).toMillis());
        execution.setErrorMessage(errorMessage);
        execution.setStackTrace(stackTrace);

        executionRepository.save(execution);
        log.warn("Execution {} failed after {} ms: {}", executionId, execution.getDurationMs(), errorMessage);
    }

    @Transactional(readOnly = true)
    public List<JobExecutionResponse> getExecutionsForJob(UUID jobId) {
        return executionRepository.findByJobIdOrderByAttemptNumberAsc(jobId).stream()
                .map(exec -> {
                    List<JobExecutionLogEntity> logs = logRepository.findByExecutionIdOrderByTimestampAsc(exec.getId());
                    return JobExecutionResponse.fromEntity(exec, logs);
                })
                .collect(Collectors.toList());
    }
}
