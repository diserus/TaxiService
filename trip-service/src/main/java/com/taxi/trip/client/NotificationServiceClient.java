package com.taxi.trip.client;

import com.taxi.trip.client.dto.NotificationCreateRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface NotificationServiceClient {

    @PostExchange("/notifications")
    void enqueue(@RequestBody NotificationCreateRequest request);
}
