package com.hsis.registrarapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DegreeRequirementRequestDto(
        @NotBlank(message = "Program code is required.") String programCode,
        @NotBlank(message = "Program name is required.") String programName,
        @NotBlank(message = "Category is required (CORE or ELECTIVE).") String category,
        Long courseId,
        @Min(value = 1, message = "Required credits must be at least 1.") Integer creditsRequired,
        String description
) {}