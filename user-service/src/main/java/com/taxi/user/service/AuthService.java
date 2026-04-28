package com.taxi.user.service;

import com.taxi.user.dto.LoginRequest;
import com.taxi.user.dto.LoginResponse;
import com.taxi.user.entity.Driver;
import com.taxi.user.entity.Passenger;
import com.taxi.user.repository.DriverRepository;
import com.taxi.user.repository.PassengerRepository;
import com.taxi.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        Passenger passenger = passengerRepository.findByEmail(email).orElse(null);
        if (passenger != null && passwordEncoder.matches(password, passenger.getPasswordHash())) {
            return buildToken(passenger.getId(), email, "PASSENGER");
        }

        Driver driver = driverRepository.findByEmail(email).orElse(null);
        if (driver != null && passwordEncoder.matches(password, driver.getPasswordHash())) {
            return buildToken(driver.getId(), email, "DRIVER");
        }

        throw new BadCredentialsException("Invalid email or password");
    }

    private LoginResponse buildToken(long userId, String email, String role) {
        String token = jwtService.issue(userId, email, role);
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtService.getExpirationMs());
        return response;
    }
}
