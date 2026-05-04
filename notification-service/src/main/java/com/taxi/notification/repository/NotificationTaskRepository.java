package com.taxi.notification.repository;

import com.taxi.notification.entity.NotificationStatus;
import com.taxi.notification.entity.NotificationTask;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    List<NotificationTask> findAllByTripId(Long tripId);

    List<NotificationTask> findAllByStatus(NotificationStatus status);

    List<NotificationTask> findAllByRecipientId(Long recipientId);

    /**
     * Атомарно захватить одну PENDING-задачу для обработки. SELECT FOR UPDATE
     * SKIP LOCKED гарантирует, что параллельные воркеры никогда не возьмут
     * одну и ту же запись — каждый получит свою задачу либо пустой результат.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("""
            SELECT t FROM NotificationTask t
            WHERE t.status = com.taxi.notification.entity.NotificationStatus.PENDING
            ORDER BY t.id
            LIMIT 1
            """)
    Optional<NotificationTask> lockNextPending();
}
