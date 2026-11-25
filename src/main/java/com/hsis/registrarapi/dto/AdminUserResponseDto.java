package com.hsis.registrarapi.dto;

import java.util.List;

public record AdminUserResponseDto(
        Long id,
        String fullName,
        String email,
        boolean active,
        List<String> roles
) {}