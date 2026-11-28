package com.hsis.registrarapi.dto;

import java.util.List;

public record BatchGradeUploadResponseDto(
        Long courseId,
        String courseCode,
        int totalRowsProcessed,
        int updatedCount,
        int errorCount,
        List<BatchGradeRowResultDto> results
) {}