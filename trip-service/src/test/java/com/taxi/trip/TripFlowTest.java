package com.taxi.trip;

import com.taxi.trip.client.dto.DriverDto;
import com.taxi.trip.dto.RatingRequest;
import com.taxi.trip.dto.TripRequest;
import com.taxi.trip.dto.TripStatus;
import com.taxi.trip.dto.TripStatusRequest;
import com.taxi.trip.repository.TripRepository;
import com.taxi.trip.service.NotificationGateway;
import com.taxi.trip.service.UserGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TripFlowTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TripRepository tripRepository;

    @MockitoBean UserGateway userGateway;
    @MockitoBean NotificationGateway notificationGateway;

    private String bearer;
    private final AtomicLong driverIdSeq = new AtomicLong();

    @BeforeEach
    void setUp() {
        tripRepository.deleteAll();
        bearer = "Bearer " + TestJwt.issue(JWT_SECRET, "passenger@mail.ru", "PASSENGER");

        driverIdSeq.set(0);
        when(userGateway.reserveDriver()).thenAnswer(inv -> {
            long id = driverIdSeq.incrementAndGet();
            return new DriverDto(id, "Driver " + id, "drv" + id + "@mail.ru",
                    "+790000" + id, "LIC" + id, "BUSY");
        });
        doNothing().when(notificationGateway).enqueue(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void protectedEndpointRequiresJwt() throws Exception {
        mockMvc.perform(get("/trips/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTripFullFlow() throws Exception {
        TripRequest req = new TripRequest(42L, "ул. Ленина, 1", "пр. Мира, 20", 10.0);

        // create
        var result = mockMvc.perform(post("/trips")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.passenger_id").value(42))
                .andExpect(jsonPath("$.driver_id").value(1))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.distance_km").value(10.0))
                .andExpect(jsonPath("$.price").value(250.0))
                .andReturn();

        Long tripId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // ASSIGNED -> ACCEPTED
        mockMvc.perform(patch("/trips/" + tripId + "/status")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TripStatusRequest(TripStatus.ACCEPTED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // ACCEPTED -> IN_PROGRESS
        mockMvc.perform(patch("/trips/" + tripId + "/status")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TripStatusRequest(TripStatus.IN_PROGRESS))))
                .andExpect(status().isOk());

        // rating before COMPLETED -> 409
        mockMvc.perform(post("/trips/" + tripId + "/rating")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequest(5))))
                .andExpect(status().isConflict());

        // IN_PROGRESS -> COMPLETED
        mockMvc.perform(patch("/trips/" + tripId + "/status")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TripStatusRequest(TripStatus.COMPLETED))))
                .andExpect(status().isOk());

        // rating now allowed
        mockMvc.perform(post("/trips/" + tripId + "/rating")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));

        // statistics for today should reflect 1 completed trip with revenue 250
        mockMvc.perform(get("/statistics").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_trips").value(1))
                .andExpect(jsonPath("$.completed_trips").value(1))
                .andExpect(jsonPath("$.cancelled_trips").value(0))
                .andExpect(jsonPath("$.total_revenue").value(250.0))
                .andExpect(jsonPath("$.average_rating").value(5.0));

        verify(userGateway, atLeastOnce()).reserveDriver();
        verify(notificationGateway, atLeastOnce())
                .enqueue(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void invalidStatusTransitionReturns409() throws Exception {
        TripRequest req = new TripRequest(7L, "A", "B", 5.0);
        var result = mockMvc.perform(post("/trips")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        Long tripId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // ASSIGNED -> COMPLETED is illegal
        mockMvc.perform(patch("/trips/" + tripId + "/status")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TripStatusRequest(TripStatus.COMPLETED))))
                .andExpect(status().isConflict());
    }

    @Test
    void noAvailableDriversReturns409() throws Exception {
        doThrow(new com.taxi.trip.exception.ConflictException("No available drivers"))
                .when(userGateway).reserveDriver();

        TripRequest req = new TripRequest(1L, "A", "B", 3.0);
        mockMvc.perform(post("/trips")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void validationErrorReturns400() throws Exception {
        // distance_km is required & minimum 0.1; passenger_id minimum 1
        String body = "{\"passenger_id\": 0, \"origin\": \"\", \"destination\": \"B\", \"distance_km\": 0.0}";
        mockMvc.perform(post("/trips")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getMissingTripReturns404() throws Exception {
        mockMvc.perform(get("/trips/99999").header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
