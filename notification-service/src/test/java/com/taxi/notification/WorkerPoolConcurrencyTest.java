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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(WorkerPoolConcurrencyTest.RecordingDeliveryConfig.class)
class WorkerPoolConcurrencyTest extends AbstractIntegrationTest {

    @Autowired NotificationTaskRepository repository;
    @Autowired RecordingDelivery delivery;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        delivery.processed.clear();
        delivery.totalCalls.set(0);
    }

    @Test
    void manyTasksAreProcessedExactlyOnce() {
        int total = 50;
        for (int i = 0; i < total; i++) {
            repository.save(NotificationTask.builder()
                    .tripId((long) (i + 1))
                    .recipientType(RecipientType.PASSENGER)
                    .recipientId(100L + i)
                    .message("msg " + i)
                    .status(NotificationStatus.PENDING)
                    .attempts((short) 0)
                    .build());
        }

        Awaitility.await().atMost(Duration.ofSeconds(15)).until(() ->
                repository.findAllByStatus(NotificationStatus.SENT).size() == total);

        // Каждая задача обработана РОВНО ОДИН раз — это и есть ключевая
        // гарантия SKIP LOCKED + IN_PROGRESS-маркировки в одной транзакции.
        assertEquals(total, delivery.processed.size(), "each task delivered exactly once");
        assertEquals(total, delivery.totalCalls.get(), "no duplicate deliveries across workers");

        // Все задачи в SENT, ни одной PENDING/IN_PROGRESS
        assertTrue(repository.findAllByStatus(NotificationStatus.PENDING).isEmpty());
        assertTrue(repository.findAllByStatus(NotificationStatus.IN_PROGRESS).isEmpty());
    }

    static class RecordingDelivery implements DeliveryGateway {
        final Set<Long> processed = ConcurrentHashMap.newKeySet();
        final AtomicLong totalCalls = new AtomicLong();

        @Override
        public void deliver(NotificationTask task) {
            totalCalls.incrementAndGet();
            if (!processed.add(task.getId())) {
                // Дублирующая доставка одной и той же задачи — провал инварианта
                throw new DeliveryException("Duplicate delivery for task " + task.getId());
            }
        }
    }

    @TestConfiguration
    static class RecordingDeliveryConfig {
        @Bean
        @Primary
        RecordingDelivery recordingDeliveryGateway() {
            return new RecordingDelivery();
        }
    }
}
