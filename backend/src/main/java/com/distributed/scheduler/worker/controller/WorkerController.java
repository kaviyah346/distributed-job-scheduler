package com.distributed.scheduler.worker.controller;

import com.distributed.scheduler.common.dto.ApiResponse;
import com.distributed.scheduler.worker.dto.WorkerResponse;
import com.distributed.scheduler.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerRepository workerRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> getAllWorkers() {
        List<WorkerResponse> workers = workerRepository.findAll().stream()
                .map(WorkerResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(workers));
    }
}
