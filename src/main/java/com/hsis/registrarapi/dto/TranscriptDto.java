package com.hsis.registrarapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TranscriptDto(
        Long studentId,
        String studentName,
        String studentEmail,
        int attemptedCredits,
        int earnedCredits,
        double totalQualityPoints,
        double cumulativeGpa,
        String academicStanding,
        LocalDateTime generatedAt,
        List<EnrollmentResponseDto> records
) {}