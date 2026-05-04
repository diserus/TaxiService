package com.taxi.trip.service;

import com.taxi.trip.client.NotificationServiceClient;
import com.taxi.trip.client.dto.NotificationCreateRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationGateway {

    private final NotificationServiceClient client;

    @Retry(name = "notificationService")
    @CircuitBreaker(name = "notificationService")
    public void enqueue(Long tripId, String recipientType, Long recipientId, String message) {
        try {
            client.enqueue(new NotificationCreateRequest(tripId, recipientType, recipientId, message));
        } catch (Exception ex) {
            // Уведомления — best-effort: ошибка не должна откатывать создание поездки
            log.warn("Failed to enqueue notification for trip {}: {}", tripId, ex.getMessage());
        }
    }
}
