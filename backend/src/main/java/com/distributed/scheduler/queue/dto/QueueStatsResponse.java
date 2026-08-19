package com.distributed.scheduler.queue.dto;

import com.distributed.scheduler.common.enums.JobStatus;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatsResponse {
    private UUID queueId;
    private String queueName;
    private boolean isPaused;
    private int concurrencyLimit;
    /** Live job counts keyed by JobStatus name. */
    private Map<String, Long> jobCounts;
}
