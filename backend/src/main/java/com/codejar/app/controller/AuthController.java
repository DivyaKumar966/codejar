package com.codejar.app.controller;

import com.codejar.app.dto.AuthResponse;
import com.codejar.app.dto.LoginRequest;
import com.codejar.app.dto.LoginResponse;
import com.codejar.app.dto.RegisterRequest;
import com.codejar.app.entity.User;
import com.codejar.app.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.registerUser(request);

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = userService.loginUser(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin-login")
    public ResponseEntity<LoginResponse> loginAdmin(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(userService.loginAdmin(request));
    }
}