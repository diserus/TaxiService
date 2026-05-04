package com.taxi.notification.service;

import com.taxi.notification.dto.NotificationRequest;
import com.taxi.notification.dto.NotificationResponse;
import com.taxi.notification.dto.NotificationStatus;
import com.taxi.notification.entity.NotificationTask;
import com.taxi.notification.exception.NotFoundException;
import com.taxi.notification.mapper.NotificationMapper;
import com.taxi.notification.repository.NotificationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTaskRepository repository;
    private final NotificationMapper mapper;

    @Transactional
    public NotificationResponse enqueue(NotificationRequest request) {
        NotificationTask task = NotificationTask.builder()
                .tripId(request.getTripId())
                .recipientType(mapper.mapRecipient(request.getRecipientType()))
                .recipientId(request.getRecipientId())
                .message(request.getMessage())
                .status(com.taxi.notification.entity.NotificationStatus.PENDING)
                .attempts((short) 0)
                .build();
        repository.save(task);
        return mapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Notification " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long tripId, NotificationStatus status, Long recipientId) {
        List<NotificationTask> tasks;
        if (status != null) {
            tasks = repository.findAllByStatus(mapper.mapStatus(status));
        } else if (tripId != null) {
            tasks = repository.findAllByTripId(tripId);
        } else if (recipientId != null) {
            tasks = repository.findAllByRecipientId(recipientId);
        } else {
            tasks = repository.findAll();
        }
        return tasks.stream().map(mapper::toResponse).toList();
    }
}
