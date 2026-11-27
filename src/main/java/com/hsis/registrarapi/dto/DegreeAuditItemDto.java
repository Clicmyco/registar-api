package com.hsis.registrarapi.dto;

public record DegreeAuditItemDto(
        Long requirementId,
        String category,
        Long courseId,
        String courseCode,
        String courseTitle,
        String description,
        int creditsRequired,
        int creditsEarned,
        int creditsInProgress,
        String status,
        String gradeAchieved
) {}