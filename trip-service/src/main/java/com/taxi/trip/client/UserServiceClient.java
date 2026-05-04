package com.taxi.trip.client;

import com.taxi.trip.client.dto.DriverDto;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface UserServiceClient {

    /**
     * Атомарно резервирует свободного водителя в user-service.
     * Возвращает DriverDto при успехе; HTTP 409 -> исключение.
     */
    @PostExchange("/drivers/assign")
    DriverDto assignAvailableDriver();
}
