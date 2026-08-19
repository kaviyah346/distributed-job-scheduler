package com.distributed.scheduler.common.enums;

public enum JobStatus {
    QUEUED,
    SCHEDULED,
    CLAIMED,
    RUNNING,
    COMPLETED,
    FAILED,
    DEAD_LETTERED
}
