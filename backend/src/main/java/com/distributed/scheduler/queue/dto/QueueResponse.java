package com.distributed.scheduler.queue.dto;

import com.distributed.scheduler.queue.entity.QueueEntity;
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
public class QueueResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;
    private String name;
    private int priority;
    private int concurrencyLimit;
    private boolean isPaused;
    private UUID retryPolicyId;
    private String retryPolicyName;
    private Instant createdAt;
    private Instant updatedAt;

    public static QueueResponse fromEntity(QueueEntity entity) {
        return QueueResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .projectName(entity.getProject().getName())
                .name(entity.getName())
                .priority(entity.getPriority())
                .concurrencyLimit(entity.getConcurrencyLimit())
                .isPaused(entity.isPaused())
                .retryPolicyId(entity.getRetryPolicy() != null ? entity.getRetryPolicy().getId() : null)
                .retryPolicyName(entity.getRetryPolicy() != null ? entity.getRetryPolicy().getName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
