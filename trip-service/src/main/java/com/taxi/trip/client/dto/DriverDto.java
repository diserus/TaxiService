package com.taxi.trip.client.dto;

public record DriverDto(
        Long id,
        String name,
        String email,
        String phone,
        String licenseNumber,
        String status
) { }
