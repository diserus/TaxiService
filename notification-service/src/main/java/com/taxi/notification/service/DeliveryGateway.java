package com.taxi.notification.service;

import com.taxi.notification.entity.NotificationTask;

/**
 * Внешний канал доставки (SMS/push/email). В демо-реализации просто логирует;
 * в тестах можно подменить, чтобы провоцировать сбои и проверять retry/DEAD.
 */
public interface DeliveryGateway {

    /**
     * @throws DeliveryException если внешний канал не смог принять сообщение
     */
    void deliver(NotificationTask task);
}
