package com.taxi.notification.worker;

import com.taxi.notification.entity.NotificationStatus;
import com.taxi.notification.entity.NotificationTask;
import com.taxi.notification.repository.NotificationTaskRepository;
import com.taxi.notification.service.DeliveryException;
import com.taxi.notification.service.DeliveryGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Транзакционные хелперы для воркеров. Каждый метод работает в собственной
 * транзакции (REQUIRES_NEW), чтобы поллинг и доставка не висели в одной
 * длинной транзакции с залоченной строкой.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskClaimer {

    private final NotificationTaskRepository repository;
    private final DeliveryGateway delivery;
    private final WorkerProperties properties;

    /**
     * Захватить одну PENDING-задачу и сразу перевести её в IN_PROGRESS.
     * Возвращает id захваченной задачи или пустой Optional, если очередь пуста.
     * SKIP LOCKED исключает гонку между параллельными воркерами.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> claimNext() {
        return repository.lockNextPending().map(task -> {
            task.setStatus(NotificationStatus.IN_PROGRESS);
            return task.getId();
        });
    }

    /**
     * Обработать ранее захваченную задачу: вызвать внешний канал доставки и
     * перевести в SENT, либо учесть провал — вернуть в PENDING для retry или
     * перевести в DEAD при исчерпании попыток.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long taskId) {
        NotificationTask task = repository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != NotificationStatus.IN_PROGRESS) {
            // защита от рассинхрона: задача удалена или уже не наша
            return;
        }
        task.setAttempts((short) (task.getAttempts() + 1));
        try {
            delivery.deliver(task);
            task.setStatus(NotificationStatus.SENT);
            task.setLastError(null);
        } catch (DeliveryException ex) {
            String err = ex.getMessage();
            task.setLastError(truncate(err));
            if (task.getAttempts() >= properties.maxAttempts()) {
                task.setStatus(NotificationStatus.DEAD);
                log.warn("Notification {} reached max attempts ({}); marking DEAD",
                        taskId, properties.maxAttempts());
            } else {
                // оставляем для повторной попытки
                task.setStatus(NotificationStatus.PENDING);
                log.info("Notification {} delivery failed (attempt {}/{}): {}",
                        taskId, task.getAttempts(), properties.maxAttempts(), err);
            }
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= 1000 ? s : s.substring(0, 1000);
    }
}
