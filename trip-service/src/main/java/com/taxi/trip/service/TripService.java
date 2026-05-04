package com.taxi.trip.service;

import com.taxi.trip.client.dto.DriverDto;
import com.taxi.trip.dto.RatingRequest;
import com.taxi.trip.dto.StatisticsResponse;
import com.taxi.trip.dto.TripRequest;
import com.taxi.trip.dto.TripResponse;
import com.taxi.trip.dto.TripStatusRequest;
import com.taxi.trip.entity.Trip;
import com.taxi.trip.entity.TripStatus;
import com.taxi.trip.exception.ConflictException;
import com.taxi.trip.exception.NotFoundException;
import com.taxi.trip.mapper.TripMapper;
import com.taxi.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository repository;
    private final TripMapper mapper;
    private final UserGateway userGateway;
    private final NotificationGateway notificationGateway;

    @Value("${trip.tariff-rate}")
    private BigDecimal tariffRate;

    @Transactional
    public TripResponse create(TripRequest request) {
        DriverDto driver = userGateway.reserveDriver();

        BigDecimal distance = BigDecimal.valueOf(request.getDistanceKm());
        BigDecimal price = distance.multiply(tariffRate).setScale(2, RoundingMode.HALF_UP);

        Trip trip = Trip.builder()
                .passengerId(request.getPassengerId())
                .driverId(driver.id())
                .status(TripStatus.ASSIGNED)
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .distanceKm(distance.setScale(2, RoundingMode.HALF_UP))
                .price(price)
                .build();
        repository.save(trip);

        notificationGateway.enqueue(trip.getId(), "DRIVER", driver.id(),
                "New trip assigned: #" + trip.getId());
        notificationGateway.enqueue(trip.getId(), "PASSENGER", request.getPassengerId(),
                "Driver " + driver.name() + " is on the way");

        return mapper.toResponse(trip);
    }

    @Transactional(readOnly = true)
    public TripResponse get(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Trip " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<TripResponse> list(Long passengerId, Long driverId, TripStatus status) {
        List<Trip> trips;
        if (status != null) {
            trips = repository.findAllByStatus(status);
        } else if (passengerId != null) {
            trips = repository.findAllByPassengerId(passengerId);
        } else if (driverId != null) {
            trips = repository.findAllByDriverId(driverId);
        } else {
            trips = repository.findAll();
        }
        return trips.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public TripResponse updateStatus(Long id, TripStatusRequest request) {
        Trip trip = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip " + id + " not found"));
        TripStatus next = mapper.map(request.getStatus());
        if (!trip.getStatus().canTransitionTo(next)) {
            throw new ConflictException(
                    "Cannot transition trip " + id + " from " + trip.getStatus() + " to " + next);
        }
        trip.setStatus(next);

        notificationGateway.enqueue(trip.getId(), "PASSENGER", trip.getPassengerId(),
                "Trip #" + trip.getId() + " status: " + next);
        if (trip.getDriverId() != null) {
            notificationGateway.enqueue(trip.getId(), "DRIVER", trip.getDriverId(),
                    "Trip #" + trip.getId() + " status: " + next);
        }
        return mapper.toResponse(trip);
    }

    @Transactional
    public TripResponse rate(Long id, RatingRequest request) {
        Trip trip = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip " + id + " not found"));
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new ConflictException(
                    "Can only rate COMPLETED trips, current status: " + trip.getStatus());
        }
        trip.setRating(request.getRating().shortValue());
        return mapper.toResponse(trip);
    }

    @Transactional(readOnly = true)
    public StatisticsResponse dailyStatistics(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now(ZoneOffset.UTC);
        OffsetDateTime from = target.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = from.plusDays(1);
        Object[] row = repository.aggregateStatistics(from, to).get(0);

        long total = ((Number) row[0]).longValue();
        long completed = ((Number) row[1]).longValue();
        long cancelled = ((Number) row[2]).longValue();
        Number revenueRaw = (Number) row[3];
        Double avgRating = row[4] == null ? null : ((Number) row[4]).doubleValue();

        StatisticsResponse response = new StatisticsResponse();
        response.setDate(target);
        response.setTotalTrips(total);
        response.setCompletedTrips(completed);
        response.setCancelledTrips(cancelled);
        response.setTotalRevenue(revenueRaw == null ? 0.0 : revenueRaw.doubleValue());
        response.setAverageRating(avgRating);
        return response;
    }
}
