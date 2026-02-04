package com.ishan.portfolio_risk_model.service;

import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import com.ishan.portfolio_risk_model.domain.repository.UserRepository;
import com.ishan.portfolio_risk_model.dto.AuthResponse;
import com.ishan.portfolio_risk_model.dto.LoginRequest;
import com.ishan.portfolio_risk_model.dto.RegisterRequest;
import com.ishan.portfolio_risk_model.security.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Service that handles user registration and authentication
@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Register a new user
    public AuthResponse register(RegisterRequest registerRequest) {
        // check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // create new user entity
        UserEntity user = new UserEntity();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword())); // hash password
        user.setRole(UserEntity.Role.USER);  // default role

        // save to database
        userRepository.save(user);

        // generate token
        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user.getEmail());
    }

    // authenticate a user (login)
    public AuthResponse login(LoginRequest loginRequest) {
        // use spring security AuthenticationManager.
        // This will:
        // 1. Call UserDetailsService.loadUserByUsername()
        // 2. Compare passwords using PasswordEncoder
        // 3. Throw exception if authentication fails
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // if we get to this point auth was succesful
        UserEntity user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate JWT token
        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user.getEmail());
    }
}
