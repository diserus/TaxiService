package com.taxi.notification.worker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkerPool {

    private final WorkerProperties properties;
    private final TaskClaimer claimer;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    @PostConstruct
    void start() {
        int size = properties.poolSize();
        executor = Executors.newFixedThreadPool(size, r -> {
            Thread t = new Thread(r);
            t.setName("notification-worker-" + t.threadId());
            t.setDaemon(true);
            return t;
        });
        running.set(true);
        for (int i = 0; i < size; i++) {
            executor.submit(this::loop);
        }
        log.info("WorkerPool started with {} workers (poll {} ms, max attempts {})",
                size, properties.pollIntervalMs(), properties.maxAttempts());
    }

    @PreDestroy
    void stop() throws InterruptedException {
        log.info("WorkerPool shutting down...");
        running.set(false);
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Workers did not finish in 30s — forcing shutdown");
            executor.shutdownNow();
        }
        log.info("WorkerPool stopped");
    }

    private void loop() {
        while (running.get()) {
            try {
                Optional<Long> claimed = claimer.claimNext();
                if (claimed.isPresent()) {
                    claimer.process(claimed.get());
                } else {
                    Thread.sleep(properties.pollIntervalMs());
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                // ловим всё, чтобы один сбой не убил воркер; короткая пауза
                log.error("Worker error: {}", ex.getMessage(), ex);
                try {
                    Thread.sleep(properties.pollIntervalMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
