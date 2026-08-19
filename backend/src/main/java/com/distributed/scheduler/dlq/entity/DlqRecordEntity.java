package com.distributed.scheduler.dlq.entity;

import com.distributed.scheduler.job.entity.JobEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dlq_records", indexes = {
        @Index(name = "idx_dlq_job", columnList = "job_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private JobEntity job;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "total_attempts", nullable = false)
    private int totalAttempts;

    @CreationTimestamp
    @Column(name = "dead_lettered_at", nullable = false, updatable = false)
    private Instant deadLetteredAt;
}
