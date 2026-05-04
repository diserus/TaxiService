package com.taxi.trip.controller;

import com.taxi.trip.api.StatisticsApi;
import com.taxi.trip.dto.StatisticsResponse;
import com.taxi.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class StatisticsController implements StatisticsApi {

    private final TripService service;

    @Override
    public ResponseEntity<StatisticsResponse> getDailyStatistics(LocalDate date) {
        return ResponseEntity.ok(service.dailyStatistics(date));
    }
}
