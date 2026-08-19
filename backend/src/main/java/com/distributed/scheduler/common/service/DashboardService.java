package com.distributed.scheduler.common.service;

import com.distributed.scheduler.common.dto.DashboardStatsResponse;
import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.common.enums.WorkerStatus;
import com.distributed.scheduler.job.repository.JobRepository;
import com.distributed.scheduler.project.repository.ProjectRepository;
import com.distributed.scheduler.queue.repository.QueueRepository;
import com.distributed.scheduler.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final QueueRepository queueRepository;
    private final WorkerRepository workerRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (JobStatus status : JobStatus.values()) {
            statusBreakdown.put(status.name(), jobRepository.countByStatus(status));
        }

        long queued = statusBreakdown.getOrDefault(JobStatus.QUEUED.name(), 0L);
        long running = statusBreakdown.getOrDefault(JobStatus.RUNNING.name(), 0L);
        long completed = statusBreakdown.getOrDefault(JobStatus.COMPLETED.name(), 0L);
        long failed = statusBreakdown.getOrDefault(JobStatus.FAILED.name(), 0L);
        long dlq = statusBreakdown.getOrDefault(JobStatus.DEAD_LETTERED.name(), 0L);
        long scheduled = statusBreakdown.getOrDefault(JobStatus.SCHEDULED.name(), 0L);
        long totalJobs = jobRepository.count();

        long totalProjects = projectRepository.count();
        long totalQueues = queueRepository.count();
        long activeWorkers = workerRepository.countByStatusIn(List.of(WorkerStatus.ACTIVE, WorkerStatus.BUSY));

        return DashboardStatsResponse.builder()
                .totalJobs(totalJobs)
                .queuedJobs(queued)
                .runningJobs(running)
                .completedJobs(completed)
                .failedJobs(failed)
                .deadLetteredJobs(dlq)
                .scheduledJobs(scheduled)
                .totalProjects(totalProjects)
                .totalQueues(totalQueues)
                .activeWorkers(activeWorkers)
                .statusBreakdown(statusBreakdown)
                .build();
    }
}
