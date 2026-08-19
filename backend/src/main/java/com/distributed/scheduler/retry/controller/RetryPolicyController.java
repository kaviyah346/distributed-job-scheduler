package com.distributed.scheduler.retry.controller;

import com.distributed.scheduler.common.dto.ApiResponse;
import com.distributed.scheduler.retry.dto.CreateRetryPolicyRequest;
import com.distributed.scheduler.retry.dto.RetryPolicyResponse;
import com.distributed.scheduler.retry.service.RetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/retry-policies")
@RequiredArgsConstructor
public class RetryPolicyController {

    private final RetryService retryService;

    @PostMapping
    public ResponseEntity<ApiResponse<RetryPolicyResponse>> create(@Valid @RequestBody CreateRetryPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Retry policy created", retryService.createRetryPolicy(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RetryPolicyResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(retryService.getRetryPolicyById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RetryPolicyResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(retryService.getAllRetryPolicies()));
    }
}
