package com.taxi.trip.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationCreateRequest(
        @JsonProperty("trip_id") Long tripId,
        @JsonProperty("recipient_type") String recipientType,
        @JsonProperty("recipient_id") Long recipientId,
        @JsonProperty("message") String message
) { }
