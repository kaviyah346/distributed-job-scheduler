package com.distributed.scheduler.execution.controller;

import com.distributed.scheduler.common.dto.ApiResponse;
import com.distributed.scheduler.execution.dto.JobExecutionResponse;
import com.distributed.scheduler.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobExecutionResponse>>> getExecutionsForJob(@PathVariable UUID jobId) {
        List<JobExecutionResponse> response = executionService.getExecutionsForJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
