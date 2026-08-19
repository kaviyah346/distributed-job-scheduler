package com.distributed.scheduler.execution.entity;

import com.distributed.scheduler.common.enums.LogLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_execution_logs", indexes = {
        @Index(name = "idx_logs_execution", columnList = "execution_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private JobExecutionEntity execution;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 20)
    private LogLevel logLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;
}
