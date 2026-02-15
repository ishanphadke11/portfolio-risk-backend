package com.ishan.portfolio_risk_model.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import com.ishan.portfolio_risk_model.domain.repository.HoldingsRepository;
import com.ishan.portfolio_risk_model.domain.repository.UserRepository;
import com.ishan.portfolio_risk_model.dto.HoldingsRequest;
import com.ishan.portfolio_risk_model.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for HoldingController.
 * Tests the full request/response cycle including authentication and database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HoldingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    private ObjectMapper objectMapper;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        holdingsRepository.deleteAll();
        userRepository.deleteAll();

        // Register and get token
        RegisterRequest registerRequest = new RegisterRequest("holder@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    @DisplayName("GET /api/v1/holdings - Should return empty list initially")
    void getHoldings_noHoldings_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/holdings - Should return 401 without token")
    void getHoldings_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/holdings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/holdings - Should create new holding")
    void createHolding_validRequest_returns201() throws Exception {
        HoldingsRequest request = new HoldingsRequest("AAPL", new BigDecimal("10.5"));

        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.quantity").value(10.5))
                .andExpect(jsonPath("$.id").isNumber());

        // Verify in database
        assertThat(holdingsRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/v1/holdings - Should reject duplicate ticker")
    void createHolding_duplicateTicker_returns400() throws Exception {
        HoldingsRequest request = new HoldingsRequest("GOOGL", new BigDecimal("5.0"));

        // First create
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate create
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/holdings - Should reject invalid quantity")
    void createHolding_negativeQuantity_returns400() throws Exception {
        HoldingsRequest request = new HoldingsRequest("TSLA", new BigDecimal("-5.0"));

        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/holdings - Should reject empty ticker")
    void createHolding_emptyTicker_returns400() throws Exception {
        HoldingsRequest request = new HoldingsRequest("", new BigDecimal("10.0"));

        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/holdings - Should return all user holdings")
    void getHoldings_withHoldings_returnsList() throws Exception {
        // Create two holdings
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldingsRequest("AAPL", new BigDecimal("10.0")))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldingsRequest("GOOGL", new BigDecimal("5.0")))))
                .andExpect(status().isCreated());

        // Get all
        mockMvc.perform(get("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("PUT /api/v1/holdings/{id} - Should update holding quantity")
    void updateHolding_validRequest_returns200() throws Exception {
        // Create holding
        HoldingsRequest createRequest = new HoldingsRequest("MSFT", new BigDecimal("10.0"));
        MvcResult createResult = mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long holdingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Update holding
        HoldingsRequest updateRequest = new HoldingsRequest("MSFT", new BigDecimal("25.0"));
        mockMvc.perform(put("/api/v1/holdings/" + holdingId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(25.0));
    }

    @Test
    @DisplayName("PUT /api/v1/holdings/{id} - Should return 400 for non-existent holding")
    void updateHolding_notFound_returns400() throws Exception {
        HoldingsRequest updateRequest = new HoldingsRequest("MSFT", new BigDecimal("25.0"));

        mockMvc.perform(put("/api/v1/holdings/99999")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/holdings/{id} - Should delete holding")
    void deleteHolding_validId_returns204() throws Exception {
        // Create holding
        HoldingsRequest createRequest = new HoldingsRequest("NFLX", new BigDecimal("3.0"));
        MvcResult createResult = mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long holdingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Delete holding
        mockMvc.perform(delete("/api/v1/holdings/" + holdingId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify deleted
        assertThat(holdingsRepository.findById(holdingId)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/holdings/{id} - Should return 400 for non-existent holding")
    void deleteHolding_notFound_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/holdings/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("User should not see other user's holdings")
    void getHoldings_differentUser_returnsOnlyOwnHoldings() throws Exception {
        // Create holding for first user
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldingsRequest("AAPL", new BigDecimal("10.0")))))
                .andExpect(status().isCreated());

        // Register second user
        RegisterRequest registerRequest2 = new RegisterRequest("holder2@example.com", "password123");
        MvcResult result2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest2)))
                .andExpect(status().isCreated())
                .andReturn();

        String token2 = objectMapper.readTree(result2.getResponse().getContentAsString()).get("token").asText();

        // Second user should see empty list
        mockMvc.perform(get("/api/v1/holdings")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
