package com.distributed.scheduler.job.dto;

import com.distributed.scheduler.common.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionContext {
    private UUID jobId;
    private UUID executionId;
    private String jobType;
    private Map<String, Object> payload;
    private int attemptNumber;
    private String workerId;

    @Builder.Default
    private List<ExecutionLogEntry> inMemoryLogs = new ArrayList<>();

    private BiConsumer<LogLevel, String> logCallback;

    public void logInfo(String message) {
        log(LogLevel.INFO, message);
    }

    public void logWarn(String message) {
        log(LogLevel.WARN, message);
    }

    public void logError(String message) {
        log(LogLevel.ERROR, message);
    }

    public void logDebug(String message) {
        log(LogLevel.DEBUG, message);
    }

    private void log(LogLevel level, String message) {
        inMemoryLogs.add(new ExecutionLogEntry(level, message));
        if (logCallback != null) {
            logCallback.accept(level, message);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecutionLogEntry {
        private LogLevel level;
        private String message;
    }
}
