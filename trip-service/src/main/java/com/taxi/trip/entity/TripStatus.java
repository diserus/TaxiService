package com.taxi.trip.entity;

import java.util.Map;
import java.util.Set;

public enum TripStatus {
    SEARCHING,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Map<TripStatus, Set<TripStatus>> ALLOWED = Map.of(
            SEARCHING,   Set.of(ASSIGNED, CANCELLED),
            ASSIGNED,    Set.of(ACCEPTED, CANCELLED),
            ACCEPTED,    Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED, CANCELLED),
            COMPLETED,   Set.of(),
            CANCELLED,   Set.of()
    );

    public boolean canTransitionTo(TripStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
