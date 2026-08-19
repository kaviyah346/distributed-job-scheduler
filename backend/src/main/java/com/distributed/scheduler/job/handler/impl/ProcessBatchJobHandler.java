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
public class ProcessBatchJobHandler implements JobHandler {

    public static final String JOB_TYPE = "PROCESS_BATCH";

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public JobResult execute(JobExecutionContext context) throws Exception {
        Map<String, Object> payload = context.getPayload();
        String batchId = payload != null && payload.containsKey("batchId")
                ? String.valueOf(payload.get("batchId")) : "BATCH-UNKNOWN";
        int itemCount = payload != null && payload.containsKey("itemCount")
                ? ((Number) payload.get("itemCount")).intValue() : 100;

        log.info("[Worker: {}] Processing batch '{}' ({} items) for job: {}",
                context.getWorkerId(), batchId, itemCount, context.getJobId());

        context.logInfo("Loading batch '" + batchId + "' with " + itemCount + " items...");
        Thread.sleep(30);

        context.logInfo("Validating item schema...");
        Thread.sleep(30);

        // Optional failure simulation
        if (payload != null && Boolean.TRUE.equals(payload.get("shouldFail"))) {
            context.logError("Batch validation failed — aborting processing");
            throw new RuntimeException("PROCESS_BATCH failed: schema validation error for batch " + batchId);
        }

        context.logInfo("Processing items in chunks of 10...");
        int successCount = (int) (itemCount * 0.97);
        int failCount = itemCount - successCount;
        Thread.sleep(50);

        context.logInfo(String.format("Batch complete — %d succeeded, %d failed", successCount, failCount));

        return JobResult.success("Batch " + batchId + " processed", Map.of(
                "batchId", batchId,
                "totalItems", itemCount,
                "successCount", successCount,
                "failCount", failCount,
                "durationMs", ThreadLocalRandom.current().nextInt(400, 900),
                "completedAt", Instant.now().toString()
        ));
    }
}
