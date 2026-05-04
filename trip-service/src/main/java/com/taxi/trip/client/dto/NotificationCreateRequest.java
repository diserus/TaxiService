package com.taxi.trip.client.dto;

public record NotificationCreateRequest(
        Long tripId,
        String recipientType,
        Long recipientId,
        String message
) { }
