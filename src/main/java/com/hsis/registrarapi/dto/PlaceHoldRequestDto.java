package com.hsis.registrarapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceHoldRequestDto(
        @NotNull(message = "Student ID is required.") Long studentId,
        @NotBlank(message = "Hold type is required.") String holdType,
        @NotBlank(message = "Reason is required.") String reason
) {}