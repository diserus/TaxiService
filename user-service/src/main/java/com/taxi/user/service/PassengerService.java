package com.taxi.user.service;

import com.taxi.user.dto.PassengerRequest;
import com.taxi.user.dto.PassengerResponse;
import com.taxi.user.entity.Passenger;
import com.taxi.user.exception.ConflictException;
import com.taxi.user.exception.NotFoundException;
import com.taxi.user.mapper.UserMapper;
import com.taxi.user.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PassengerResponse create(PassengerRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Passenger with email " + request.getEmail() + " already exists");
        }
        Passenger passenger = Passenger.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        repository.save(passenger);
        return mapper.toResponse(passenger);
    }

    @Transactional(readOnly = true)
    public PassengerResponse get(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Passenger " + id + " not found"));
    }
}
