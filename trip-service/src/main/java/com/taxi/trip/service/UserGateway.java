package com.taxi.trip.service;

import com.taxi.trip.client.UserServiceClient;
import com.taxi.trip.client.dto.DriverDto;
import com.taxi.trip.exception.ConflictException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class UserGateway {

    private final UserServiceClient client;

    @Retry(name = "userService")
    @CircuitBreaker(name = "userService")
    public DriverDto reserveDriver() {
        try {
            return client.assignAvailableDriver();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new ConflictException("No available drivers");
            }
            throw ex;
        }
    }
}
