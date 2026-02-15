package com.ishan.portfolio_risk_model.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishan.portfolio_risk_model.domain.repository.UserRepository;
import com.ishan.portfolio_risk_model.dto.LoginRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests the full request/response cycle including database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should register new user")
    void register_validRequest_returns201WithToken() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // Verify user was saved to database
        assertThat(userRepository.existsByEmail("newuser@example.com")).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject duplicate email")
    void register_duplicateEmail_returns400() throws Exception {
        // First registration
        RegisterRequest request = new RegisterRequest("duplicate@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject invalid email")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("notanemail", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject short password")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("user@example.com", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should login existing user")
    void login_validCredentials_returns200WithToken() throws Exception {
        // First register
        RegisterRequest registerRequest = new RegisterRequest("login@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest loginRequest = new LoginRequest("login@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject wrong password")
    void login_wrongPassword_returns401() throws Exception {
        // First register
        RegisterRequest registerRequest = new RegisterRequest("wrongpw@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest("wrongpw@example.com", "wrongpassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject non-existent user")
    void login_nonExistentUser_returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nouser@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token from registration should be valid")
    void register_tokenShouldBeUsable() throws Exception {
        RegisterRequest request = new RegisterRequest("tokentest@example.com", "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        // Token should have 3 parts (header.payload.signature)
        assertThat(token.split("\\.")).hasSize(3);
    }
}
