package com.cleancity.cleancity.service;

import com.cleancity.cleancity.dto.LoginRequestDto;
import com.cleancity.cleancity.dto.RegisterRequestDto;
import com.cleancity.cleancity.entity.Role;
import com.cleancity.cleancity.entity.User;
import com.cleancity.cleancity.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // REGISTER USER
    public User registerUser(RegisterRequestDto request) {

        // Check whether email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        // Create new user
        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Encode password before saving
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Set default role
        user.setRole(Role.CITIZEN);

        // Save user in database
        return userRepository.save(user);
    }

    // LOGIN USER
    public User loginUser(LoginRequestDto request) {

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        )
                );

        // Compare entered password with encoded password
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        // Return authenticated user
        return user;
    }

}