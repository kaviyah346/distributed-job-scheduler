package com.distributed.scheduler.job.handler;

import com.distributed.scheduler.job.dto.JobExecutionContext;
import com.distributed.scheduler.job.dto.JobResult;

public interface JobHandler {
    
    /**
     * Unique job type string (e.g. SEND_EMAIL, GENERATE_REPORT, CLEANUP_DATA).
     */
    String getJobType();

    /**
     * Executes the unit of work given the runtime execution context.
     */
    JobResult execute(JobExecutionContext context) throws Exception;
}
