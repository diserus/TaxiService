package com.taxi.trip.repository;

import com.taxi.trip.entity.Trip;
import com.taxi.trip.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findAllByPassengerId(Long passengerId);

    List<Trip> findAllByDriverId(Long driverId);

    List<Trip> findAllByStatus(TripStatus status);

    @Query("""
            SELECT COUNT(t),
                   COUNT(CASE WHEN t.status = com.taxi.trip.entity.TripStatus.COMPLETED THEN 1 END),
                   COUNT(CASE WHEN t.status = com.taxi.trip.entity.TripStatus.CANCELLED THEN 1 END),
                   COALESCE(SUM(CASE WHEN t.status = com.taxi.trip.entity.TripStatus.COMPLETED THEN t.price ELSE 0 END), 0),
                   AVG(t.rating)
            FROM Trip t
            WHERE t.createdAt >= :from AND t.createdAt < :to
            """)
    List<Object[]> aggregateStatistics(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
