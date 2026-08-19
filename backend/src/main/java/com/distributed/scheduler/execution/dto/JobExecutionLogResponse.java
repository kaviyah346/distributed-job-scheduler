package com.distributed.scheduler.execution.dto;

import com.distributed.scheduler.common.enums.LogLevel;
import com.distributed.scheduler.execution.entity.JobExecutionLogEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionLogResponse {
    private UUID id;
    private LogLevel logLevel;
    private String message;
    private Instant timestamp;

    public static JobExecutionLogResponse fromEntity(JobExecutionLogEntity entity) {
        return JobExecutionLogResponse.builder()
                .id(entity.getId())
                .logLevel(entity.getLogLevel())
                .message(entity.getMessage())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
