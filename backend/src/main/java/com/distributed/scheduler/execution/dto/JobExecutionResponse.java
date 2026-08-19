package com.distributed.scheduler.execution.dto;

import com.distributed.scheduler.common.enums.ExecutionStatus;
import com.distributed.scheduler.common.enums.LogLevel;
import com.distributed.scheduler.execution.entity.JobExecutionEntity;
import com.distributed.scheduler.execution.entity.JobExecutionLogEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionResponse {
    private UUID id;
    private UUID jobId;
    private String workerId;
    private int attemptNumber;
    private ExecutionStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private String errorMessage;
    private String stackTrace;
    private Map<String, Object> resultOutput;
    private List<JobExecutionLogResponse> logs;

    public static JobExecutionResponse fromEntity(JobExecutionEntity entity, List<JobExecutionLogEntity> logs) {
        return JobExecutionResponse.builder()
                .id(entity.getId())
                .jobId(entity.getJob().getId())
                .workerId(entity.getWorkerId())
                .attemptNumber(entity.getAttemptNumber())
                .status(entity.getStatus())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .durationMs(entity.getDurationMs())
                .errorMessage(entity.getErrorMessage())
                .stackTrace(entity.getStackTrace())
                .resultOutput(entity.getResultOutput())
                .logs(logs != null ? logs.stream().map(JobExecutionLogResponse::fromEntity).toList() : List.of())
                .build();
    }
}
