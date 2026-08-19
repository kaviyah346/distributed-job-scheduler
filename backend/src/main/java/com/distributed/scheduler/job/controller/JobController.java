package com.distributed.scheduler.job.controller;

import com.distributed.scheduler.common.dto.ApiResponse;
import com.distributed.scheduler.common.enums.JobStatus;
import com.distributed.scheduler.job.dto.CreateJobRequest;
import com.distributed.scheduler.job.dto.JobResponse;
import com.distributed.scheduler.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(@Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job submitted successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(@PathVariable UUID id) {
        JobResponse response = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getJobs(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID queueId,
            @RequestParam(required = false) JobStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<JobResponse> page = jobService.getJobs(projectId, queueId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<JobResponse>> cancelJob(@PathVariable UUID id) {
        JobResponse response = jobService.cancelJob(id);
        return ResponseEntity.ok(ApiResponse.success("Job cancelled successfully", response));
    }
}
