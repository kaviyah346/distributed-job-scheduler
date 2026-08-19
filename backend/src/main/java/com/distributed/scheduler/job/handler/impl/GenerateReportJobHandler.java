package com.distributed.scheduler.job.handler.impl;

import com.distributed.scheduler.job.dto.JobExecutionContext;
import com.distributed.scheduler.job.dto.JobResult;
import com.distributed.scheduler.job.handler.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class GenerateReportJobHandler implements JobHandler {

    public static final String JOB_TYPE = "GENERATE_REPORT";

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public JobResult execute(JobExecutionContext context) throws Exception {
        Map<String, Object> payload = context.getPayload();
        String reportType = payload != null && payload.containsKey("reportType")
                ? String.valueOf(payload.get("reportType")) : "SUMMARY";
        String format = payload != null && payload.containsKey("format")
                ? String.valueOf(payload.get("format")) : "PDF";

        log.info("[Worker: {}] Generating {} report (format: {}) for job: {}",
                context.getWorkerId(), reportType, format, context.getJobId());

        context.logInfo("Initializing report generator for type: " + reportType);
        Thread.sleep(50);

        context.logInfo("Querying data source for report period...");
        Thread.sleep(80);

        // Optional failure simulation
        if (payload != null && Boolean.TRUE.equals(payload.get("shouldFail"))) {
            context.logError("Data source unavailable — report generation aborted");
            throw new RuntimeException("Report generation failed: data source timeout for type " + reportType);
        }

        context.logInfo("Rendering " + format + " document...");
        Thread.sleep(50);

        String reportId = "RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        context.logInfo("Report rendered successfully. ID: " + reportId);

        return JobResult.success("Report generated: " + reportId, Map.of(
                "reportId", reportId,
                "reportType", reportType,
                "format", format,
                "generatedAt", Instant.now().toString(),
                "sizeKb", 142
        ));
    }
}
