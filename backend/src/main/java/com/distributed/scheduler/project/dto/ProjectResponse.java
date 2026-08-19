package com.distributed.scheduler.project.dto;

import com.distributed.scheduler.project.entity.ProjectEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String apiKey;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProjectResponse fromEntity(ProjectEntity entity) {
        return ProjectResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .apiKey(entity.getApiKey())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
