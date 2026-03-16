package com.ishan.portfolio_risk_model.config;

import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import com.ishan.portfolio_risk_model.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates a default test user on startup if one doesn't already exist.
 * Remove this class once registration is working correctly.
 */
@Component
@AllArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String testEmail = "test@factorlens.com";

        // always upsert so credentials are always known
        UserEntity user = userRepository.findByEmail(testEmail).orElseGet(UserEntity::new);
        user.setEmail(testEmail);
        user.setPasswordHash(passwordEncoder.encode("Test1234!"));
        user.setRole(UserEntity.Role.USER);
        userRepository.save(user);
    }
}
