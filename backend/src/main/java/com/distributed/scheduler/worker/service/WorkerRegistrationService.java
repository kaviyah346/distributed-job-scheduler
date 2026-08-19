package com.distributed.scheduler.worker.service;

import com.distributed.scheduler.common.enums.WorkerStatus;
import com.distributed.scheduler.worker.entity.WorkerEntity;
import com.distributed.scheduler.worker.repository.WorkerRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerRegistrationService {

    private final WorkerRepository workerRepository;

    @Value("${app.worker.thread-pool-size:10}")
    private int maxConcurrency;

    @Getter
    private String workerId;
    private String hostname;
    private final AtomicInteger activeJobs = new AtomicInteger(0);

    @PostConstruct
    @Transactional
    public void registerWorker() {
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            this.hostname = "localhost";
        }
        this.workerId = "worker-" + hostname + "-" + UUID.randomUUID().toString().substring(0, 8);

        WorkerEntity worker = WorkerEntity.builder()
                .id(this.workerId)
                .hostname(this.hostname)
                .status(WorkerStatus.ACTIVE)
                .currentJobCount(0)
                .maxConcurrency(this.maxConcurrency)
                .lastHeartbeatAt(Instant.now())
                .build();

        workerRepository.save(worker);
        log.info("Registered worker '{}' with max concurrency: {}", this.workerId, this.maxConcurrency);
    }

    @Scheduled(fixedDelayString = "${app.worker.heartbeat-interval-ms:5000}")
    @Transactional
    public void sendHeartbeat() {
        if (workerId == null) return;
        workerRepository.findById(workerId).ifPresent(worker -> {
            worker.setLastHeartbeatAt(Instant.now());
            worker.setCurrentJobCount(activeJobs.get());
            worker.setStatus(activeJobs.get() >= maxConcurrency ? WorkerStatus.BUSY : WorkerStatus.ACTIVE);
            workerRepository.save(worker);
        });
    }

    public void incrementActiveJobs() {
        activeJobs.incrementAndGet();
    }

    public void decrementActiveJobs() {
        activeJobs.decrementAndGet();
    }

    public int getAvailableCapacity() {
        return Math.max(0, maxConcurrency - activeJobs.get());
    }

    @PreDestroy
    @Transactional
    public void unregisterWorker() {
        if (workerId != null) {
            workerRepository.findById(workerId).ifPresent(worker -> {
                worker.setStatus(WorkerStatus.STOPPED);
                worker.setLastHeartbeatAt(Instant.now());
                workerRepository.save(worker);
                log.info("Worker '{}' gracefully stopped.", workerId);
            });
        }
    }
}
