package com.distributed.scheduler.queue.controller;

import com.distributed.scheduler.common.dto.ApiResponse;
import com.distributed.scheduler.queue.dto.CreateQueueRequest;
import com.distributed.scheduler.queue.dto.QueueResponse;
import com.distributed.scheduler.queue.dto.QueueStatsResponse;
import com.distributed.scheduler.queue.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping
    public ResponseEntity<ApiResponse<QueueResponse>> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        QueueResponse response = queueService.createQueue(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Queue created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QueueResponse>> getQueue(@PathVariable UUID id) {
        QueueResponse response = queueService.getQueueById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QueueResponse>>> getQueues(
            @RequestParam(required = false) UUID projectId) {
        List<QueueResponse> response = (projectId != null)
                ? queueService.getQueuesByProject(projectId)
                : queueService.getAllQueues();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<QueueStatsResponse>> getQueueStats(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(queueService.getQueueStats(id)));
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<QueueResponse>> pauseQueue(@PathVariable UUID id) {
        QueueResponse response = queueService.pauseQueue(id);
        return ResponseEntity.ok(ApiResponse.success("Queue paused", response));
    }

    @PutMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<QueueResponse>> resumeQueue(@PathVariable UUID id) {
        QueueResponse response = queueService.resumeQueue(id);
        return ResponseEntity.ok(ApiResponse.success("Queue resumed", response));
    }
}
