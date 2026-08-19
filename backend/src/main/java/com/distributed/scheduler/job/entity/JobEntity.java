package com.distributed.scheduler.job.entity;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.project.entity.ProjectEntity;
import com.distributed.scheduler.queue.entity.QueueEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_claim", columnList = "status, scheduled_at, priority, created_at"),
        @Index(name = "idx_jobs_queue_status", columnList = "queue_id, status"),
        @Index(name = "idx_jobs_project", columnList = "project_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "queue_id", nullable = false)
    private QueueEntity queue;

    @Column(name = "job_type", nullable = false, length = 100)
    private String jobType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private JobStatus status = JobStatus.QUEUED;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 1;

    @Column(name = "current_retry_count", nullable = false)
    @Builder.Default
    private int currentRetryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    @Column(name = "scheduled_at", nullable = false)
    @Builder.Default
    private Instant scheduledAt = Instant.now();

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "locked_by_worker_id", length = 100)
    private String lockedByWorkerId;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
