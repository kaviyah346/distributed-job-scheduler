package com.distributed.scheduler.retry.entity;

import com.distributed.scheduler.common.enums.RetryStrategy;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retry_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RetryStrategy strategy = RetryStrategy.EXPONENTIAL_BACKOFF;

    /** Maximum number of retry attempts before moving to DLQ. */
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    /** Initial delay in seconds before the first retry. */
    @Column(name = "initial_interval_seconds", nullable = false)
    @Builder.Default
    private int initialIntervalSeconds = 10;

    /** Maximum backoff cap in seconds (used for exponential backoff). */
    @Column(name = "max_interval_seconds", nullable = false)
    @Builder.Default
    private int maxIntervalSeconds = 300;

    /** Multiplier applied per attempt for exponential backoff (e.g. 2.0). */
    @Column(name = "backoff_multiplier", nullable = false)
    @Builder.Default
    private double backoffMultiplier = 2.0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
