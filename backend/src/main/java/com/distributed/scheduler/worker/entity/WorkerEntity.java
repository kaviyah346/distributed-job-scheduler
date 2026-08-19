package com.distributed.scheduler.worker.entity;

import com.distributed.scheduler.common.enums.WorkerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "workers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerEntity {

    @Id
    @Column(nullable = false, length = 100)
    private String id;

    @Column(nullable = false, length = 255)
    private String hostname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private WorkerStatus status = WorkerStatus.ACTIVE;

    @Column(name = "current_job_count", nullable = false)
    @Builder.Default
    private int currentJobCount = 0;

    @Column(name = "max_concurrency", nullable = false)
    @Builder.Default
    private int maxConcurrency = 10;

    @Column(name = "last_heartbeat_at", nullable = false)
    private Instant lastHeartbeatAt;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;
}
