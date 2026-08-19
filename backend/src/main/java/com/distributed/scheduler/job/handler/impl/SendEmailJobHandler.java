package com.distributed.scheduler.job.handler.impl;

import com.distributed.scheduler.job.dto.JobExecutionContext;
import com.distributed.scheduler.job.dto.JobResult;
import com.distributed.scheduler.job.handler.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class SendEmailJobHandler implements JobHandler {

    public static final String JOB_TYPE = "SEND_EMAIL";

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public JobResult execute(JobExecutionContext context) throws Exception {
        Map<String, Object> payload = context.getPayload();
        String to = payload != null && payload.containsKey("to") ? String.valueOf(payload.get("to")) : "user@example.com";
        String subject = payload != null && payload.containsKey("subject") ? String.valueOf(payload.get("subject")) : "Notification";

        context.logInfo("Initializing SMTP transmission for recipient: " + to);
        log.info("[Worker: {}] Executing SEND_EMAIL for Job: {}, Recipient: {}, Attempt: {}", 
                context.getWorkerId(), context.getJobId(), to, context.getAttemptNumber());

        // Simulated network transmission delay
        Thread.sleep(300);

        context.logInfo("Connecting to mail relay server...");
        Thread.sleep(200);

        // Optional failure simulation for retry testing
        if (payload != null && Boolean.TRUE.equals(payload.get("shouldFail"))) {
            context.logError("Simulated SMTP timeout while sending to " + to);
            throw new RuntimeException("SMTP Connection Timeout: Failed to deliver email to " + to);
        }

        context.logInfo("Message successfully accepted by mail relay server. Message-ID generated.");
        String messageId = "msg-" + UUID.randomUUID().toString().substring(0, 8);

        return JobResult.success("Email successfully delivered to " + to, Map.of(
                "recipient", to,
                "subject", subject,
                "messageId", messageId,
                "deliveredAt", java.time.Instant.now().toString()
        ));
    }
}
