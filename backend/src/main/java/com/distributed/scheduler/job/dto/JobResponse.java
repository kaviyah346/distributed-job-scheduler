package com.distributed.scheduler.job.dto;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.job.entity.JobEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;
    private UUID queueId;
    private String queueName;
    private String jobType;
    private Map<String, Object> payload;
    private JobStatus status;
    private int priority;
    private int currentRetryCount;
    private int maxRetries;
    private Instant scheduledAt;
    private String cronExpression;
    private String lockedByWorkerId;
    private Instant lockedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static JobResponse fromEntity(JobEntity entity) {
        return JobResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .projectName(entity.getProject().getName())
                .queueId(entity.getQueue().getId())
                .queueName(entity.getQueue().getName())
                .jobType(entity.getJobType())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .currentRetryCount(entity.getCurrentRetryCount())
                .maxRetries(entity.getMaxRetries())
                .scheduledAt(entity.getScheduledAt())
                .cronExpression(entity.getCronExpression())
                .lockedByWorkerId(entity.getLockedByWorkerId())
                .lockedAt(entity.getLockedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
