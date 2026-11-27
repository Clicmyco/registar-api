package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.AdminUserResponseDto;
import com.hsis.registrarapi.entity.Role;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.RoleRepository;
import com.hsis.registrarapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {

    private static final Set<String> VALID_ROLES = Set.of(
            "ROLE_STUDENT",
            "ROLE_ADVISOR",
            "ROLE_REGISTRAR",
            "ROLE_ADMIN",
            "ROLE_INSTRUCTOR"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AdminUserResponseDto updateUserRoles(Long userId, Set<String> roleNames, String actingAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Normalize and validate role names (accepts "INSTRUCTOR" or "ROLE_INSTRUCTOR")
        Set<Role> resolvedRoles = new HashSet<>();
        for (String rawName : roleNames) {
            String normalized = rawName.trim().toUpperCase();
            if (!normalized.startsWith("ROLE_")) {
                normalized = "ROLE_" + normalized;
            }
            String finalName = normalized;

            if (!VALID_ROLES.contains(finalName)) {
                throw new RuntimeException("Invalid role: " + finalName);
            }

            Role role = roleRepository.findByName(finalName)
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName(finalName);
                        return roleRepository.save(newRole);
                    });
            resolvedRoles.add(role);
        }

        // Guardrail: prevent an Admin from accidentally removing their own ADMIN role
        boolean isSelf = user.getEmail().equalsIgnoreCase(actingAdminEmail);
        boolean keepsAdmin = resolvedRoles.stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (isSelf && !keepsAdmin) {
            throw new RuntimeException("You cannot remove ROLE_ADMIN from your own account.");
        }

        user.setRoles(resolvedRoles);
        User saved = userRepository.save(user);

        List<String> updatedNames = saved.getRoles().stream().map(Role::getName).sorted().toList();
        auditLogService.log(
                "USER_ROLES_UPDATED",
                "User",
                "Updated roles for " + saved.getEmail() + " to " + updatedNames
        );

        return toDto(saved);
    }

    @Transactional
    public AdminUserResponseDto updateUserStatus(Long userId, boolean active, String actingAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (user.getEmail().equalsIgnoreCase(actingAdminEmail) && !active) {
            throw new RuntimeException("You cannot deactivate your own account.");
        }

        user.setActive(active);
        User saved = userRepository.save(user);

        auditLogService.log(
                active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "User",
                (active ? "Activated" : "Deactivated") + " account " + saved.getEmail()
        );

        return toDto(saved);
    }

    private AdminUserResponseDto toDto(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();

        return new AdminUserResponseDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                roleNames
        );
    }
}