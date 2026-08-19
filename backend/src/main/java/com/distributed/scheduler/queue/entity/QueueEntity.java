package com.distributed.scheduler.queue.entity;

import com.distributed.scheduler.project.entity.ProjectEntity;
import com.distributed.scheduler.retry.entity.RetryPolicyEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "queues", uniqueConstraints = {
        @UniqueConstraint(name = "uk_queues_project_name", columnNames = {"project_id", "name"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 1;

    @Column(name = "concurrency_limit", nullable = false)
    @Builder.Default
    private int concurrencyLimit = 5;

    @Column(name = "is_paused", nullable = false)
    @Builder.Default
    private boolean isPaused = false;

    /** Optional default retry policy applied to all jobs in this queue. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retry_policy_id")
    private RetryPolicyEntity retryPolicy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
