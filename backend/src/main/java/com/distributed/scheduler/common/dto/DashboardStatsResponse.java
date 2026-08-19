package com.distributed.scheduler.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalJobs;
    private long queuedJobs;
    private long runningJobs;
    private long completedJobs;
    private long failedJobs;
    private long deadLetteredJobs;
    private long scheduledJobs;
    private long totalProjects;
    private long totalQueues;
    private long activeWorkers;
    private Map<String, Long> statusBreakdown;
}
