package com.distributed.scheduler.job.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateJobRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Queue ID is required")
    private UUID queueId;

    @NotBlank(message = "Job type is required")
    private String jobType;

    private Map<String, Object> payload;

    @Builder.Default
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 100, message = "Priority cannot exceed 100")
    private int priority = 1;

    @Builder.Default
    @Min(value = 0, message = "Max retries cannot be negative")
    @Max(value = 20, message = "Max retries cannot exceed 20")
    private int maxRetries = 3;

    private Instant scheduledAt;

    private String cronExpression;
}
