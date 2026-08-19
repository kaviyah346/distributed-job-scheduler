package com.distributed.scheduler.job.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class JobHandlerRegistry {

    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();

    public JobHandlerRegistry(List<JobHandler> handlerList) {
        if (handlerList != null) {
            for (JobHandler handler : handlerList) {
                register(handler);
            }
        }
    }

    public void register(JobHandler handler) {
        if (handler != null && handler.getJobType() != null) {
            String key = handler.getJobType().trim().toUpperCase();
            handlers.put(key, handler);
            log.info("Registered generic JobHandler for job type: '{}' -> {}", key, handler.getClass().getSimpleName());
        }
    }

    public Optional<JobHandler> getHandler(String jobType) {
        if (jobType == null) return Optional.empty();
        return Optional.ofNullable(handlers.get(jobType.trim().toUpperCase()));
    }

    public boolean hasHandler(String jobType) {
        if (jobType == null) return false;
        return handlers.containsKey(jobType.trim().toUpperCase());
    }
}
