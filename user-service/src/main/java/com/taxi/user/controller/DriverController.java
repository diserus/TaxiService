package com.taxi.user.controller;

import com.taxi.user.api.DriversApi;
import com.taxi.user.dto.DriverRequest;
import com.taxi.user.dto.DriverResponse;
import com.taxi.user.dto.DriverStatusRequest;
import com.taxi.user.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DriverController implements DriversApi {

    private final DriverService service;

    @Override
    public ResponseEntity<DriverResponse> createDriver(DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Override
    public ResponseEntity<DriverResponse> getDriver(Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @Override
    public ResponseEntity<DriverResponse> updateDriverStatus(Long id, DriverStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @Override
    public ResponseEntity<List<DriverResponse>> listAvailableDrivers() {
        return ResponseEntity.ok(service.listAvailable());
    }

    @Override
    public ResponseEntity<DriverResponse> assignAvailableDriver() {
        return ResponseEntity.ok(service.assignAvailable());
    }
}
