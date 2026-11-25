package com.hsis.registrarapi.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentReviewRequest(
        @NotNull(message = "Approval decision (true/false) is required")
        Boolean approve,
        String advisorNote
) {}