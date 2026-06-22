package com.minipay.payment_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Thread pool configured in application.yml
    // under spring.task.execution
}
