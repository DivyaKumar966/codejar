package com.codejar.app.service;

import com.codejar.app.dto.LoginRequest;
import com.codejar.app.dto.LoginResponse;
import com.codejar.app.dto.RegisterRequest;
import com.codejar.app.entity.User;
import com.codejar.app.repository.UserRepository;
import com.codejar.app.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // =========================
    // REGISTER
    // =========================

    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Encrypt / hash password
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole("USER");

        return userRepository.save(user);
    }


    // =========================
    // LOGIN
    // =========================

    public LoginResponse loginUser(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password")
                );

        // Check entered password with hashed password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        normalizeRole(user);

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    public LoginResponse loginAdmin(LoginRequest request) {
        LoginResponse response = loginUser(request);
        if (!"ADMIN".equals(response.getRole())) {
            throw new IllegalArgumentException("Invalid admin credentials");
        }
        return response;
    }

    private void normalizeRole(User user) {
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("USER");
            userRepository.save(user);
        } else {
            user.setRole("ADMIN");
        }
    }
}