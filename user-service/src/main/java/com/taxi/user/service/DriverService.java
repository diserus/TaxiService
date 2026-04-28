package com.taxi.user.service;

import com.taxi.user.dto.DriverRequest;
import com.taxi.user.dto.DriverResponse;
import com.taxi.user.dto.DriverStatusRequest;
import com.taxi.user.entity.Driver;
import com.taxi.user.entity.DriverStatus;
import com.taxi.user.exception.ConflictException;
import com.taxi.user.exception.NotFoundException;
import com.taxi.user.mapper.UserMapper;
import com.taxi.user.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    public static final String AVAILABLE_DRIVERS_CACHE = "availableDrivers";

    private final DriverRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @CacheEvict(value = AVAILABLE_DRIVERS_CACHE, allEntries = true)
    public DriverResponse create(DriverRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Driver with email " + request.getEmail() + " already exists");
        }
        if (repository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ConflictException("Driver with license " + request.getLicenseNumber() + " already exists");
        }
        Driver driver = Driver.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .licenseNumber(request.getLicenseNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(DriverStatus.AVAILABLE)
                .build();
        repository.save(driver);
        return mapper.toResponse(driver);
    }

    @Transactional(readOnly = true)
    public DriverResponse get(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));
    }

    @Transactional
    @CacheEvict(value = AVAILABLE_DRIVERS_CACHE, allEntries = true)
    public DriverResponse updateStatus(Long id, DriverStatusRequest request) {
        Driver driver = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));
        driver.setStatus(mapper.map(request.getStatus()));
        return mapper.toResponse(driver);
    }

    @Transactional(readOnly = true)
    @Cacheable(AVAILABLE_DRIVERS_CACHE)
    public List<DriverResponse> listAvailable() {
        return repository.findAllByStatus(DriverStatus.AVAILABLE).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Атомарно резервирует одного свободного водителя. SELECT FOR UPDATE SKIP LOCKED
     * гарантирует уникальность результата при конкурентных вызовах.
     */
    @Transactional
    @CacheEvict(value = AVAILABLE_DRIVERS_CACHE, allEntries = true)
    public DriverResponse assignAvailable() {
        Driver driver = repository.lockOneAvailable()
                .orElseThrow(() -> new ConflictException("No available drivers"));
        driver.setStatus(DriverStatus.BUSY);
        return mapper.toResponse(driver);
    }
}
