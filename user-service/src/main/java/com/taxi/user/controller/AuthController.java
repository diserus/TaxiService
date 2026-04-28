package com.taxi.user.controller;

import com.taxi.user.api.AuthApi;
import com.taxi.user.dto.LoginRequest;
import com.taxi.user.dto.LoginResponse;
import com.taxi.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService service;

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }
}
