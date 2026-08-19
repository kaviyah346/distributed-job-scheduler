package com.distributed.scheduler.retry.dto;

import com.distributed.scheduler.common.enums.RetryStrategy;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRetryPolicyRequest {

    @NotBlank(message = "Retry policy name is required")
    private String name;

    @NotNull(message = "Strategy is required")
    @Builder.Default
    private RetryStrategy strategy = RetryStrategy.EXPONENTIAL_BACKOFF;

    @Min(1) @Max(20)
    @Builder.Default
    private int maxRetries = 3;

    @Min(1) @Max(3600)
    @Builder.Default
    private int initialIntervalSeconds = 10;

    @Min(1) @Max(86400)
    @Builder.Default
    private int maxIntervalSeconds = 300;

    @DecimalMin("1.0") @DecimalMax("10.0")
    @Builder.Default
    private double backoffMultiplier = 2.0;
}
