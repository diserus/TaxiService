package com.taxi.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.user.dto.LoginRequest;
import com.taxi.user.dto.PassengerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PassengerFlowTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerAndLogin() throws Exception {
        PassengerRequest req = new PassengerRequest("Иван", "ivan@mail.ru", "+79001234567", "secret123");

        mockMvc.perform(post("/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"));

        // Duplicate email -> 409
        mockMvc.perform(post("/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        // Login with seeded admin (password from V2 migration)
        LoginRequest login = new LoginRequest("admin@taxi.com", "secret");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"));

        // Wrong password -> 401
        LoginRequest bad = new LoginRequest("admin@taxi.com", "wrong");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRequiresJwt() throws Exception {
        mockMvc.perform(get("/passengers/1"))
                .andExpect(status().isUnauthorized());
    }
}
