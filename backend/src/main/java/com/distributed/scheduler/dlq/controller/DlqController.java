package com.distributed.scheduler.dlq.controller;

import com.distributed.scheduler.common.dto.ApiResponse;
import com.distributed.scheduler.dlq.dto.DlqRecordResponse;
import com.distributed.scheduler.dlq.service.DlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final DlqService dlqService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DlqRecordResponse>>> getAllDlqRecords() {
        return ResponseEntity.ok(ApiResponse.success(dlqService.getAllDlqRecords()));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<DlqRecordResponse>> getDlqRecordByJobId(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(dlqService.getDlqRecordByJobId(jobId)));
    }

    @PostMapping("/jobs/{jobId}/requeue")
    public ResponseEntity<ApiResponse<DlqRecordResponse>> requeueJob(@PathVariable UUID jobId) {
        DlqRecordResponse response = dlqService.requeueJob(jobId);
        return ResponseEntity.ok(ApiResponse.success("Job re-queued from DLQ successfully", response));
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<Void>> purgeJob(@PathVariable UUID jobId) {
        dlqService.purgeJob(jobId);
        return ResponseEntity.ok(ApiResponse.success("Job purged from DLQ", null));
    }
}
