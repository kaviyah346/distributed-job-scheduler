package com.distributed.scheduler.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResult {
    private boolean success;
    private String message;
    private Map<String, Object> outputData;
    private String errorMessage;
    private String stackTrace;

    public static JobResult success(String message) {
        return JobResult.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static JobResult success(String message, Map<String, Object> outputData) {
        return JobResult.builder()
                .success(true)
                .message(message)
                .outputData(outputData)
                .build();
    }

    public static JobResult failure(String errorMessage) {
        return JobResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static JobResult failure(String errorMessage, String stackTrace) {
        return JobResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .build();
    }
}
