package com.hsis.registrarapi.dto;

import java.time.LocalDateTime;

public record PendingEnrollmentDto(
        Long enrollmentId,
        Long studentId,
        String studentName,
        String studentEmail,
        Long courseId,
        String courseCode,
        String courseTitle,
        int credits,
        long currentEnrolled,
        int maxCapacity,
        LocalDateTime requestedAt
) {}