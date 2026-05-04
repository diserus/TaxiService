package com.taxi.notification.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.worker")
public record WorkerProperties(
        int poolSize,
        long pollIntervalMs,
        int maxAttempts
) { }
