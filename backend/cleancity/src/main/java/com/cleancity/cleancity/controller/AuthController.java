package com.cleancity.cleancity.controller;

import com.cleancity.cleancity.dto.LoginRequestDto;
import com.cleancity.cleancity.dto.LoginResponseDto;
import com.cleancity.cleancity.dto.RegisterRequestDto;
import com.cleancity.cleancity.dto.RegisterResponseDto;
import com.cleancity.cleancity.entity.User;
import com.cleancity.cleancity.security.JwtService;
import com.cleancity.cleancity.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    // REGISTER API
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request) {

        User user = userService.registerUser(request);

        RegisterResponseDto response = new RegisterResponseDto(
                "Registration successful",
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // LOGIN API
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request) {

        User user = userService.loginUser(request);

        String token = jwtService.generateToken(user.getEmail());

        LoginResponseDto response = new LoginResponseDto(
                "Login successful",
                user.getEmail(),
                user.getRole().name(),
                token
        );

        return ResponseEntity.ok(response);
    }
}