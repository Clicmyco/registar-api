package com.hsis.registrarapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    // This endpoint is NOT in /api/auth, so it is automatically protected!
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/profile")
    
    public ResponseEntity<String> getStudentProfile() {
        return ResponseEntity.ok("Welcome to the secure student portal! Your JWT was successfully verified.");
    }
}