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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class TripService {

    private final TripRepository repository;
    private final TripMapper mapper;
    private final UserGateway userGateway;
    private final NotificationGateway notificationGateway;

    @Value("${trip.tariff-rate}")
    private BigDecimal tariffRate;

    @Value("${trip.assigned-ttl-seconds:60}")
    private long assignedTtlSeconds;

    public record CreateResult(TripResponse trip, boolean created) {}

    @Transactional
    public CreateResult create(TripRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = repository.findByPassengerIdAndIdempotencyKey(
                    request.getPassengerId(), idempotencyKey);
            if (existing.isPresent()) {
                return new CreateResult(mapper.toResponse(existing.get()), false);
            }
        }

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
                .idempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null)
                .build();
        repository.save(trip);

        notificationGateway.enqueue(trip.getId(), "DRIVER", driver.id(),
                "New trip assigned: #" + trip.getId());
        notificationGateway.enqueue(trip.getId(), "PASSENGER", request.getPassengerId(),
                "Driver " + driver.name() + " is on the way");

        return new CreateResult(mapper.toResponse(trip), true);
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

    /**
     * Отменяет поездки, которые застряли в ASSIGNED дольше TTL (пассажир пропал/не подтвердил),
     * и возвращает водителя в AVAILABLE. Вызывается скедулером.
     */
    @Transactional
    public int cancelStuckAssigned() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(assignedTtlSeconds);
        List<Trip> stuck = repository.findStuckAssigned(threshold);
        int cancelled = 0;
        for (Trip trip : stuck) {
            if (trip.getStatus() != TripStatus.ASSIGNED) {
                continue;
            }
            // Сначала освобождаем водителя — если упало, поездка останется ASSIGNED и
            // скедулер повторит попытку на следующем тике.
            if (trip.getDriverId() != null) {
                try {
                    userGateway.releaseDriver(trip.getDriverId());
                } catch (Exception ex) {
                    log.warn("Skip cancelling trip {}: failed to release driver {}: {}",
                            trip.getId(), trip.getDriverId(), ex.getMessage());
                    continue;
                }
            }

            trip.setStatus(TripStatus.CANCELLED);
            cancelled++;

            notificationGateway.enqueue(trip.getId(), "PASSENGER", trip.getPassengerId(),
                    "Trip #" + trip.getId() + " was cancelled (not accepted in time)");
            if (trip.getDriverId() != null) {
                notificationGateway.enqueue(trip.getId(), "DRIVER", trip.getDriverId(),
                        "Trip #" + trip.getId() + " was cancelled");
            }
        }
        if (cancelled > 0) {
            log.info("Cancelled {} stuck ASSIGNED trips (TTL {}s)", cancelled, assignedTtlSeconds);
        }
        return cancelled;
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
