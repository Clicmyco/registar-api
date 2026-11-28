package com.hsis.registrarapi.dto;

import java.time.LocalDateTime;

public record AcademicHoldDto(
        Long id,
        Long studentId,
        String studentName,
        String studentEmail,
        String holdType,
        String reason,
        boolean active,
        String placedByEmail,
        LocalDateTime placedAt,
        String resolvedByEmail,
        LocalDateTime resolvedAt,
        String resolutionNote
) {}