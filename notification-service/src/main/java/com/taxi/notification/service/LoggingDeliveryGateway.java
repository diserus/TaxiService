package com.taxi.notification.service;

import com.taxi.notification.entity.NotificationTask;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingDeliveryGateway implements DeliveryGateway {

    @Override
    public void deliver(NotificationTask task) {
        log.info("[deliver] -> {} #{} (trip {}): {}",
                task.getRecipientType(), task.getRecipientId(),
                task.getTripId(), task.getMessage());
    }
}
