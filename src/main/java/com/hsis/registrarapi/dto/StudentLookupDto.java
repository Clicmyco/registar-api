package com.hsis.registrarapi.dto;

public record StudentLookupDto(
        Long id,
        String fullName,
        String email,
        long activeHoldCount
) {}