package com.ishan.portfolio_risk_model.domain.repository;

import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new UserEntity();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword123");
        testUser.setRole(UserEntity.Role.USER);
    }

    @Test
    @DisplayName("Should save and retrieve user by email")
    void findByEmail_existingUser_returnsUser() {
        userRepository.save(testUser);

        Optional<UserEntity> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getRole()).isEqualTo(UserEntity.Role.USER);
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_nonExistingUser_returnsEmpty() {
        Optional<UserEntity> found = userRepository.findByEmail("notfound@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if email exists")
    void existsByEmail_existingEmail_returnsTrue() {
        userRepository.save(testUser);

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }
}
