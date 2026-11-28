package com.hsis.registrarapi.dto;

public record BatchGradeRowResultDto(
        int rowNumber,
        String identifier,
        String studentName,
        String grade,
        String status,
        String message
) {}