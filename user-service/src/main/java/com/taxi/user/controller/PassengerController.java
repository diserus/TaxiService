package com.taxi.user.controller;

import com.taxi.user.api.PassengersApi;
import com.taxi.user.dto.PassengerRequest;
import com.taxi.user.dto.PassengerResponse;
import com.taxi.user.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PassengerController implements PassengersApi {

    private final PassengerService service;

    @Override
    public ResponseEntity<PassengerResponse> createPassenger(PassengerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Override
    public ResponseEntity<PassengerResponse> getPassenger(Long id) {
        return ResponseEntity.ok(service.get(id));
    }
}
