package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.UserRegistrationDto;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import com.hsis.registrarapi.dto.ForgotPasswordDto;
import com.hsis.registrarapi.dto.ResetPasswordDto;
import com.hsis.registrarapi.dto.UserLoginDto;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    // Dependency Injection: Hand the Waiter a connection to the Kitchen Manager
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // This tells the Waiter to listen for POST requests at /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDto registrationData) {
        try {
            // Hand the DTO to the Service layer
            User savedUser = userService.registerUser(registrationData);
            
            // If successful, return a 200 OK HTTP Status and the saved user data
            return ResponseEntity.ok(savedUser);
            
        } catch (RuntimeException e) {
            // If the Service throws our "Email already exists" error, return a 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDto loginData) {
        try {
            // Get the generated token from the Service
            String token = userService.loginUser(loginData);
            
            // Package the token into a clean JSON response format: { "token": "eyJhb..." }
            return ResponseEntity.ok(Map.of("token", token));
            
        } catch (RuntimeException e) {
            // 401 Unauthorized is the correct HTTP status for failed logins
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

   @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordDto request) {
        try {
            userService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok("A 6-digit confirmation code has been sent to your email.");
        } catch (RuntimeException e) {
              e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto request) {
        try {
            userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok("Password updated successfully! You can now sign in.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
}