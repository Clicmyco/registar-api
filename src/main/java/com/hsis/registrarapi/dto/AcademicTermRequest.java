package com.hsis.registrarapi.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record AcademicTermRequest(
        @NotBlank(message = "Term code is required (e.g. 2026-FALL)")
        String code,
        @NotBlank(message = "Term name is required (e.g. Fall 2026)")
        String name,
        Boolean registrationOpen,
        LocalDate addDropDeadline,
        Boolean setActive
) {}