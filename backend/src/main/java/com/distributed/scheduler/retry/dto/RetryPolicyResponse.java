package com.distributed.scheduler.retry.dto;

import com.distributed.scheduler.common.enums.RetryStrategy;
import com.distributed.scheduler.retry.entity.RetryPolicyEntity;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPolicyResponse {
    private UUID id;
    private String name;
    private RetryStrategy strategy;
    private int maxRetries;
    private int initialIntervalSeconds;
    private int maxIntervalSeconds;
    private double backoffMultiplier;
    private Instant createdAt;

    public static RetryPolicyResponse fromEntity(RetryPolicyEntity e) {
        return RetryPolicyResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .strategy(e.getStrategy())
                .maxRetries(e.getMaxRetries())
                .initialIntervalSeconds(e.getInitialIntervalSeconds())
                .maxIntervalSeconds(e.getMaxIntervalSeconds())
                .backoffMultiplier(e.getBackoffMultiplier())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
