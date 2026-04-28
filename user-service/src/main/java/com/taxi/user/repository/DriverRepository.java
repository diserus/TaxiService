package com.taxi.user.repository;

import com.taxi.user.entity.Driver;
import com.taxi.user.entity.DriverStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findAllByStatus(DriverStatus status);

    /**
     * Атомарно подобрать одного свободного водителя.
     * SELECT FOR UPDATE SKIP LOCKED гарантирует, что параллельные запросы
     * никогда не выберут одну и ту же запись — каждый получит уникального
     * водителя или пустой результат.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT d FROM Driver d WHERE d.status = com.taxi.user.entity.DriverStatus.AVAILABLE ORDER BY d.id LIMIT 1")
    Optional<Driver> lockOneAvailable();
}
