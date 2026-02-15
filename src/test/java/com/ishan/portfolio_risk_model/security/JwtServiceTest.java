package com.ishan.portfolio_risk_model.security;

import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtService.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Inject test values using reflection (simulating @Value injection)
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        // Create test user
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setRole(UserEntity.Role.USER);
    }

    @Test
    @DisplayName("Should generate a valid JWT token")
    void generateToken_validUser_returnsToken() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract email from token")
    void extractEmail_validToken_returnsEmail() {
        String token = jwtService.generateToken(testUser);

        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should validate token for correct user")
    void isTokenValid_correctUser_returnsTrue() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token for different user")
    void isTokenValid_differentUser_returnsFalse() {
        String token = jwtService.generateToken(testUser);

        UserEntity differentUser = new UserEntity();
        differentUser.setEmail("other@example.com");

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract correct expiration from token")
    void extractExpiration_validToken_returnsExpiration() {
        String token = jwtService.generateToken(testUser);

        var expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isNotNull();
        assertThat(expiration.getTime()).isGreaterThan(System.currentTimeMillis());
    }
}
