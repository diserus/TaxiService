package com.taxi.user;

import com.taxi.user.dto.DriverRequest;
import com.taxi.user.entity.DriverStatus;
import com.taxi.user.exception.ConflictException;
import com.taxi.user.repository.DriverRepository;
import com.taxi.user.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DriverAssignmentConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    DriverService driverService;

    @Autowired
    DriverRepository driverRepository;

    @BeforeEach
    void cleanup() {
        driverRepository.deleteAll();
    }

    @Test
    void concurrentAssignmentsProduceUniqueDrivers() throws Exception {
        int driverCount = 5;
        int concurrency = 10;

        for (int i = 0; i < driverCount; i++) {
            driverService.create(new DriverRequest(
                    "Driver " + i,
                    "driver" + i + "@mail.ru",
                    "+7900000000" + i,
                    "LIC" + i,
                    "secret123"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        Set<Long> assignedIds = ConcurrentHashMap.newKeySet();
        Set<String> errors = ConcurrentHashMap.newKeySet();

        Future<?>[] futures = new Future[concurrency];
        for (int i = 0; i < concurrency; i++) {
            futures[i] = pool.submit(() -> {
                start.await();
                try {
                    var assigned = driverService.assignAvailable();
                    assignedIds.add(assigned.getId());
                } catch (ConflictException ex) {
                    errors.add(ex.getMessage());
                }
                return null;
            });
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(driverCount, assignedIds.size(),
                "Each successful assignment must hand out a unique driver");
        assertEquals(driverCount, new HashSet<>(assignedIds).size(),
                "No duplicates in successful assignments");
        assertEquals(concurrency - driverCount, errors.size(),
                "Excess concurrent calls must get ConflictException 'No available drivers'");

        long busyCount = driverRepository.findAll().stream()
                .filter(d -> d.getStatus() == DriverStatus.BUSY)
                .count();
        assertEquals(driverCount, busyCount, "All drivers must end up BUSY");
    }
}
