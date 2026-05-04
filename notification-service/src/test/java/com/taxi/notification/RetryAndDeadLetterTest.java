package com.taxi.notification;

import com.taxi.notification.entity.NotificationStatus;
import com.taxi.notification.entity.NotificationTask;
import com.taxi.notification.entity.RecipientType;
import com.taxi.notification.repository.NotificationTaskRepository;
import com.taxi.notification.service.DeliveryException;
import com.taxi.notification.service.DeliveryGateway;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(RetryAndDeadLetterTest.FlakyDeliveryConfig.class)
class RetryAndDeadLetterTest extends AbstractIntegrationTest {

    @Autowired NotificationTaskRepository repository;
    @Autowired FlakyDelivery delivery;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        delivery.failuresLeft.set(0);
        delivery.totalCalls.set(0);
    }

    @Test
    void failingDeliveryRetriesUpToMaxAndMarksDead() {
        // Гейтвей всегда падает -> через 3 попытки задача должна стать DEAD
        delivery.failuresLeft.set(Integer.MAX_VALUE);

        NotificationTask task = repository.save(NotificationTask.builder()
                .tripId(1L)
                .recipientType(RecipientType.DRIVER)
                .recipientId(42L)
                .message("flaky one")
                .status(NotificationStatus.PENDING)
                .attempts((short) 0)
                .build());

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() ->
                repository.findById(task.getId())
                        .map(t -> t.getStatus() == NotificationStatus.DEAD)
                        .orElse(false));

        NotificationTask after = repository.findById(task.getId()).orElseThrow();
        assertEquals(NotificationStatus.DEAD, after.getStatus());
        assertEquals(3, after.getAttempts(), "should have tried exactly max-attempts times");
        assertNotNull(after.getLastError(), "last_error should capture failure reason");
        assertEquals(3, delivery.totalCalls.get());
    }

    @Test
    void recoveringDeliveryEventuallySucceeds() {
        // Падаем 2 раза, на 3-й — успех. Статус должен быть SENT, attempts == 3.
        delivery.failuresLeft.set(2);

        NotificationTask task = repository.save(NotificationTask.builder()
                .tripId(2L)
                .recipientType(RecipientType.PASSENGER)
                .recipientId(99L)
                .message("recovers")
                .status(NotificationStatus.PENDING)
                .attempts((short) 0)
                .build());

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() ->
                repository.findById(task.getId())
                        .map(t -> t.getStatus() == NotificationStatus.SENT)
                        .orElse(false));

        NotificationTask after = repository.findById(task.getId()).orElseThrow();
        assertEquals(NotificationStatus.SENT, after.getStatus());
        assertEquals(3, after.getAttempts());
    }

    static class FlakyDelivery implements DeliveryGateway {
        final AtomicInteger failuresLeft = new AtomicInteger(0);
        final AtomicInteger totalCalls = new AtomicInteger(0);

        @Override
        public void deliver(NotificationTask task) {
            totalCalls.incrementAndGet();
            if (failuresLeft.getAndDecrement() > 0) {
                throw new DeliveryException("simulated upstream failure");
            }
        }
    }

    @TestConfiguration
    static class FlakyDeliveryConfig {
        @Bean
        @Primary
        FlakyDelivery flakyDeliveryGateway() {
            return new FlakyDelivery();
        }
    }
}
