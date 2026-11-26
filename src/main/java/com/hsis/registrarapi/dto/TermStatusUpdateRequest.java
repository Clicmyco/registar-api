package com.hsis.registrarapi.dto;

import java.time.LocalDate;

public record TermStatusUpdateRequest(
        Boolean registrationOpen,
        Boolean active,
        LocalDate addDropDeadline
) {}