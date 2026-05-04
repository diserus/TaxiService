package com.taxi.trip.controller;

import com.taxi.trip.api.TripsApi;
import com.taxi.trip.dto.RatingRequest;
import com.taxi.trip.dto.TripRequest;
import com.taxi.trip.dto.TripResponse;
import com.taxi.trip.dto.TripStatus;
import com.taxi.trip.dto.TripStatusRequest;
import com.taxi.trip.mapper.TripMapper;
import com.taxi.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TripController implements TripsApi {

    private final TripService service;
    private final TripMapper mapper;

    @Override
    public ResponseEntity<TripResponse> createTrip(TripRequest tripRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(tripRequest));
    }

    @Override
    public ResponseEntity<TripResponse> getTrip(Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @Override
    public ResponseEntity<List<TripResponse>> listTrips(Long passengerId, Long driverId, TripStatus status) {
        return ResponseEntity.ok(service.list(passengerId, driverId, mapper.map(status)));
    }

    @Override
    public ResponseEntity<TripResponse> rateTrip(Long id, RatingRequest ratingRequest) {
        return ResponseEntity.ok(service.rate(id, ratingRequest));
    }

    @Override
    public ResponseEntity<TripResponse> updateTripStatus(Long id, TripStatusRequest tripStatusRequest) {
        return ResponseEntity.ok(service.updateStatus(id, tripStatusRequest));
    }
}
