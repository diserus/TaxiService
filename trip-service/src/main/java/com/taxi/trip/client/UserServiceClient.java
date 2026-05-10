package com.taxi.trip.client;

import com.taxi.trip.client.dto.DriverDto;
import com.taxi.trip.client.dto.DriverStatusUpdateRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface UserServiceClient {

    /**
     * Атомарно резервирует свободного водителя в user-service.
     * Возвращает DriverDto при успехе; HTTP 409 -> исключение.
     */
    @PostExchange("/drivers/assign")
    DriverDto assignAvailableDriver();

    /**
     * Обновляет статус водителя. Принимает Authorization explicitly, потому что вызов
     * может идти из фонового скедулера, где нет HTTP-контекста.
     */
    @PatchExchange("/drivers/{id}/status")
    void updateDriverStatus(@PathVariable("id") Long id,
                            @RequestBody DriverStatusUpdateRequest request,
                            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
