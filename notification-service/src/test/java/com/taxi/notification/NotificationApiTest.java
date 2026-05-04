package com.taxi.notification;

import com.taxi.notification.dto.NotificationRequest;
import com.taxi.notification.dto.RecipientType;
import com.taxi.notification.repository.NotificationTaskRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class NotificationApiTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired NotificationTaskRepository repository;

    private String bearer;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        bearer = "Bearer " + TestJwt.issue(JWT_SECRET, "user@mail.ru", "ADMIN");
    }

    @Test
    void protectedEndpointRequiresJwt() throws Exception {
        mockMvc.perform(get("/notifications/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enqueueListGet() throws Exception {
        NotificationRequest req = new NotificationRequest(100L, RecipientType.PASSENGER, 7L, "trip assigned");

        var result = mockMvc.perform(post("/notifications")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.trip_id").value(100))
                .andExpect(jsonPath("$.recipient_type").value("PASSENGER"))
                .andExpect(jsonPath("$.attempts").value(0))
                .andReturn();

        Long id = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // worker должен очень быстро доставить с poll-interval 50ms
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/notifications/" + id).header("Authorization", bearer))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("SENT"))
                        .andExpect(jsonPath("$.attempts").value(1)));

        // фильтр по trip_id
        mockMvc.perform(get("/notifications?trip_id=100").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trip_id").value(100));
    }

    @Test
    void validationErrorReturns400() throws Exception {
        String body = "{\"trip_id\":0,\"recipient_type\":\"PASSENGER\",\"recipient_id\":1,\"message\":\"\"}";
        mockMvc.perform(post("/notifications")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getMissingNotificationReturns404() throws Exception {
        mockMvc.perform(get("/notifications/99999").header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
