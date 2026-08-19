package com.distributed.scheduler.scheduler.service;

import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.job.entity.JobEntity;
import com.distributed.scheduler.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobEligibilityService {

    private final JobRepository jobRepository;

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(fixedDelayString = "${app.scheduler.poll-interval-ms:2000}")
    @Transactional
    public void promoteDueScheduledJobs() {
        if (!schedulerEnabled) return;

        Instant now = Instant.now();
        List<JobEntity> dueJobs = jobRepository.findDueScheduledJobs(now);

        if (!dueJobs.isEmpty()) {
            log.info("[Scheduler Engine] Promoting {} scheduled jobs whose due time has arrived to QUEUED", dueJobs.size());
            for (JobEntity job : dueJobs) {
                job.setStatus(JobStatus.QUEUED);
                job.setUpdatedAt(now);
                jobRepository.save(job);
                log.debug("Promoted job {} (type: {}) to QUEUED", job.getId(), job.getJobType());
            }
        }
    }
}
