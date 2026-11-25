package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.AdminUserResponseDto;
import com.hsis.registrarapi.dto.UpdateUserRolesRequest;
import com.hsis.registrarapi.dto.UpdateUserStatusRequest;
import com.hsis.registrarapi.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PatchMapping("/{id}/roles")
    public ResponseEntity<AdminUserResponseDto> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                adminUserService.updateUserRoles(id, request.roles(), authentication.getName())
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponseDto> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                adminUserService.updateUserStatus(id, request.active(), authentication.getName())
        );
    }
}