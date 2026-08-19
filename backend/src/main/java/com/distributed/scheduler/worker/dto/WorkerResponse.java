package com.distributed.scheduler.worker.dto;

import com.distributed.scheduler.common.enums.WorkerStatus;
import com.distributed.scheduler.worker.entity.WorkerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerResponse {
    private String id;
    private String hostname;
    private WorkerStatus status;
    private int currentJobCount;
    private int maxConcurrency;
    private Instant lastHeartbeatAt;
    private Instant registeredAt;

    public static WorkerResponse fromEntity(WorkerEntity entity) {
        return WorkerResponse.builder()
                .id(entity.getId())
                .hostname(entity.getHostname())
                .status(entity.getStatus())
                .currentJobCount(entity.getCurrentJobCount())
                .maxConcurrency(entity.getMaxConcurrency())
                .lastHeartbeatAt(entity.getLastHeartbeatAt())
                .registeredAt(entity.getRegisteredAt())
                .build();
    }
}
