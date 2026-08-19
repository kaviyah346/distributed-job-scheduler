package com.distributed.scheduler.dlq.dto;

import com.distributed.scheduler.dlq.entity.DlqRecordEntity;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqRecordResponse {
    private UUID id;
    private UUID jobId;
    private String jobType;
    private String queueName;
    private String reason;
    private String lastError;
    private String stackTrace;
    private int totalAttempts;
    private Instant deadLetteredAt;

    public static DlqRecordResponse fromEntity(DlqRecordEntity e) {
        return DlqRecordResponse.builder()
                .id(e.getId())
                .jobId(e.getJob().getId())
                .jobType(e.getJob().getJobType())
                .queueName(e.getJob().getQueue().getName())
                .reason(e.getReason())
                .lastError(e.getLastError())
                .stackTrace(e.getStackTrace())
                .totalAttempts(e.getTotalAttempts())
                .deadLetteredAt(e.getDeadLetteredAt())
                .build();
    }
}
