package com.ishan.portfolio_risk_model.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishan.portfolio_risk_model.domain.repository.FactorAnalysisResultsRepository;
import com.ishan.portfolio_risk_model.domain.repository.HoldingsRepository;
import com.ishan.portfolio_risk_model.domain.repository.UserRepository;
import com.ishan.portfolio_risk_model.dto.FlaskAnalysisRequest;
import com.ishan.portfolio_risk_model.dto.FlaskAnalysisResponse;
import com.ishan.portfolio_risk_model.dto.HoldingsRequest;
import com.ishan.portfolio_risk_model.dto.RegisterRequest;
import com.ishan.portfolio_risk_model.service.FlaskClient;
import com.ishan.portfolio_risk_model.service.FlaskServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private FactorAnalysisResultsRepository resultsRepository;

    @MockitoBean
    private FlaskClient flaskClient;

    private ObjectMapper objectMapper;
    private String authToken;

    private FlaskAnalysisResponse mockFlaskResponse;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        resultsRepository.deleteAll();
        holdingsRepository.deleteAll();
        userRepository.deleteAll();

        // Register and get token
        RegisterRequest registerRequest = new RegisterRequest("analyst@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("token").asText();

        // Create mock Flask response
        mockFlaskResponse = new FlaskAnalysisResponse();
        mockFlaskResponse.setAlpha(new BigDecimal("0.000234"));
        mockFlaskResponse.setBetaMkt(new BigDecimal("0.856420"));
        mockFlaskResponse.setBetaSmb(new BigDecimal("0.123456"));
        mockFlaskResponse.setBetaHml(new BigDecimal("-0.045678"));
        mockFlaskResponse.setBetaRmw(new BigDecimal("0.032145"));
        mockFlaskResponse.setBetaCma(new BigDecimal("-0.012345"));
        mockFlaskResponse.setRSquared(new BigDecimal("0.876543"));
        mockFlaskResponse.setTStats(Map.of(
                "alpha", new BigDecimal("1.2345"),
                "mkt", new BigDecimal("15.6789")
        ));
    }

    @Test
    @DisplayName("POST /api/v1/analysis/run - Should run analysis and return 200")
    void runAnalysis_withHoldings_returns200() throws Exception {
        // Create a holding first
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HoldingsRequest("AAPL", new BigDecimal("10")))))
                .andExpect(status().isCreated());

        // Mock Flask client
        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class)))
                .thenReturn(mockFlaskResponse);

        // Run analysis
        mockMvc.perform(post("/api/v1/analysis/run")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.alpha").value(0.000234))
                .andExpect(jsonPath("$.betaMkt").value(0.856420))
                .andExpect(jsonPath("$.rSquared").value(0.876543))
                .andExpect(jsonPath("$.tStats").isMap())
                .andExpect(jsonPath("$.analysisDate").isNotEmpty());

        // Verify persisted to database
        assertThat(resultsRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/v1/analysis/run - Should return 400 when no holdings")
    void runAnalysis_noHoldings_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/run")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No holdings found. Add holdings before running analysis."));
    }

    @Test
    @DisplayName("POST /api/v1/analysis/run - Should return 401 without token")
    void runAnalysis_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/run"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/analysis/run - Should accept optional date params")
    void runAnalysis_withDates_returns200() throws Exception {
        // Create a holding
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HoldingsRequest("GOOGL", new BigDecimal("5")))))
                .andExpect(status().isCreated());

        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class)))
                .thenReturn(mockFlaskResponse);

        mockMvc.perform(post("/api/v1/analysis/run")
                        .header("Authorization", "Bearer " + authToken)
                        .param("startDate", "2022-01-01")
                        .param("endDate", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alpha").value(0.000234));
    }

    @Test
    @DisplayName("POST /api/v1/analysis/run - Should return 404 when Flask reports invalid ticker")
    void runAnalysis_invalidTicker_returns404() throws Exception {
        // Create a holding
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HoldingsRequest("XYZFAKE", new BigDecimal("10")))))
                .andExpect(status().isCreated());

        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class)))
                .thenThrow(new FlaskServiceException("Invalid ticker: XYZFAKE", HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/v1/analysis/run")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Invalid ticker: XYZFAKE"));
    }

    @Test
    @DisplayName("GET /api/v1/analysis/history - Should return past results")
    void getHistory_afterRunning_returnsList() throws Exception {
        // Create holding and run analysis
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HoldingsRequest("AAPL", new BigDecimal("10")))))
                .andExpect(status().isCreated());

        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class)))
                .thenReturn(mockFlaskResponse);

        mockMvc.perform(post("/api/v1/analysis/run")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Get history
        mockMvc.perform(get("/api/v1/analysis/history")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alpha").value(0.000234));
    }

    @Test
    @DisplayName("GET /api/v1/analysis/{id} - Should return specific result")
    void getAnalysisById_found_returns200() throws Exception {
        // Create holding and run analysis
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HoldingsRequest("AAPL", new BigDecimal("10")))))
                .andExpect(status().isCreated());

        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class)))
                .thenReturn(mockFlaskResponse);

        MvcResult runResult = mockMvc.perform(post("/api/v1/analysis/run")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long analysisId = objectMapper.readTree(runResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Get by ID
        mockMvc.perform(get("/api/v1/analysis/" + analysisId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(analysisId))
                .andExpect(jsonPath("$.alpha").value(0.000234));
    }

    @Test
    @DisplayName("GET /api/v1/analysis/{id} - Should return 400 for non-existent ID")
    void getAnalysisById_notFound_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Analysis result not found"));
    }

    @Test
    @DisplayName("User should not see another user's analysis result")
    void getAnalysisById_differentUser_returns400() throws Exception {
        // Create holding and run analysis as first user
        mockMvc.perform(post("/api/v1/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HoldingsRequest("AAPL", new BigDecimal("10")))))
                .andExpect(status().isCreated());

        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class)))
                .thenReturn(mockFlaskResponse);

        MvcResult runResult = mockMvc.perform(post("/api/v1/analysis/run")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long analysisId = objectMapper.readTree(runResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Register second user
        RegisterRequest registerRequest2 = new RegisterRequest("analyst2@example.com", "password123");
        MvcResult result2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest2)))
                .andExpect(status().isCreated())
                .andReturn();

        String token2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("token").asText();

        // Second user tries to access first user's result
        mockMvc.perform(get("/api/v1/analysis/" + analysisId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Analysis result not found"));
    }
}
