package com.distributed.scheduler.job.handler.impl;

import com.distributed.scheduler.job.dto.JobExecutionContext;
import com.distributed.scheduler.job.dto.JobResult;
import com.distributed.scheduler.job.handler.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class CleanupDataJobHandler implements JobHandler {

    public static final String JOB_TYPE = "CLEANUP_DATA";

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public JobResult execute(JobExecutionContext context) throws Exception {
        Map<String, Object> payload = context.getPayload();
        String targetTable = payload != null && payload.containsKey("targetTable")
                ? String.valueOf(payload.get("targetTable")) : "audit_logs";
        int retentionDays = payload != null && payload.containsKey("retentionDays")
                ? ((Number) payload.get("retentionDays")).intValue() : 90;

        log.info("[Worker: {}] Running CLEANUP_DATA on '{}' (retention: {} days) for job: {}",
                context.getWorkerId(), targetTable, retentionDays, context.getJobId());

        context.logInfo("Connecting to database...");
        Thread.sleep(30);

        context.logInfo("Scanning table '" + targetTable + "' for records older than " + retentionDays + " days...");
        Thread.sleep(50);

        // Optional failure simulation
        if (payload != null && Boolean.TRUE.equals(payload.get("shouldFail"))) {
            context.logError("Database lock contention — cleanup aborted");
            throw new RuntimeException("CLEANUP_DATA failed: lock timeout on table " + targetTable);
        }

        int deletedRows = ThreadLocalRandom.current().nextInt(50, 5000);
        long freedBytes = (long) deletedRows * 512L;

        context.logInfo("Deleted " + deletedRows + " expired records from '" + targetTable + "'.");
        Thread.sleep(30);

        context.logInfo("VACUUM ANALYZE triggered on '" + targetTable + "'.");

        return JobResult.success("Cleanup completed on " + targetTable, Map.of(
                "targetTable", targetTable,
                "retentionDays", retentionDays,
                "deletedRows", deletedRows,
                "freedBytes", freedBytes,
                "completedAt", Instant.now().toString()
        ));
    }
}
