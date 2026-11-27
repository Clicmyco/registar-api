package com.hsis.registrarapi.dto;

import java.util.List;

public record DegreeAuditDto(
        Long studentId,
        String studentName,
        String studentEmail,
        String programCode,
        String programName,
        int totalRequiredCredits,
        int completedCredits,
        int inProgressCredits,
        int remainingCredits,
        double completionPercentage,
        boolean eligibleForGraduation,
        List<DegreeAuditItemDto> requirements
) {}