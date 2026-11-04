package com.teste.bank.infrastructure.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        return new ThreadPoolTaskExecutorBuilder()
                .corePoolSize(4)
                .maxPoolSize(8)
                .queueCapacity(500)
                .threadNamePrefix("notification-")
                .awaitTermination(true)
                .build();
    }
}
