package com.taxi.trip.scheduler;

import com.taxi.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StuckTripCleanupScheduler {

    private final TripService tripService;

    @Scheduled(fixedDelayString = "${trip.assigned-cleanup-interval-ms:30000}",
               initialDelayString = "${trip.assigned-cleanup-interval-ms:30000}")
    public void cancelStuckAssigned() {
        try {
            tripService.cancelStuckAssigned();
        } catch (Exception ex) {
            log.error("Stuck-trip cleanup tick failed", ex);
        }
    }
}
