package com.yusufkurnaz.ProjectManagementBackend.AI.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for async processing
 * Follows SRP - Single responsibility: Async configuration management
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration {

    /**
     * Task executor for AI processing operations
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-processing-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("AI processing task rejected: {}", r.toString());
            throw new RuntimeException("AI processing queue is full");
        });
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for file processing operations
     */
    @Bean(name = "fileTaskExecutor")
    public Executor fileTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("file-processing-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("File processing task rejected: {}", r.toString());
            throw new RuntimeException("File processing queue is full");
        });
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for vector operations
     */
    @Bean(name = "vectorTaskExecutor")
    public Executor vectorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(30);
        executor.setThreadNamePrefix("vector-processing-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("Vector processing task rejected: {}", r.toString());
            throw new RuntimeException("Vector processing queue is full");
        });
        executor.initialize();
        return executor;
    }
}
