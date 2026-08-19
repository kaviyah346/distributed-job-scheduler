package com.distributed.scheduler.queue.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueueRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Queue name is required")
    private String name;

    @Builder.Default
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 100, message = "Priority cannot exceed 100")
    private int priority = 1;

    @Builder.Default
    @Min(value = 1, message = "Concurrency limit must be at least 1")
    @Max(value = 500, message = "Concurrency limit cannot exceed 500")
    private int concurrencyLimit = 5;

    /** Optional retry policy UUID to attach to this queue as its default. */
    private UUID retryPolicyId;
}
