package com.taxi.notification.controller;

import com.taxi.notification.api.NotificationsApi;
import com.taxi.notification.dto.NotificationRequest;
import com.taxi.notification.dto.NotificationResponse;
import com.taxi.notification.dto.NotificationStatus;
import com.taxi.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationsApi {

    private final NotificationService service;

    @Override
    public ResponseEntity<NotificationResponse> enqueueNotification(NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.enqueue(request));
    }

    @Override
    public ResponseEntity<NotificationResponse> getNotification(Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @Override
    public ResponseEntity<List<NotificationResponse>> listNotifications(Long tripId,
                                                                         NotificationStatus status,
                                                                         Long recipientId) {
        return ResponseEntity.ok(service.list(tripId, status, recipientId));
    }
}
