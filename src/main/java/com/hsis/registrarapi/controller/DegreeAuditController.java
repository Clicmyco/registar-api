package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.DegreeAuditDto;
import com.hsis.registrarapi.dto.DegreeAuditItemDto;
import com.hsis.registrarapi.dto.DegreeRequirementRequestDto;
import com.hsis.registrarapi.service.DegreeAuditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/degree-audit")
public class DegreeAuditController {

    private final DegreeAuditService degreeAuditService;

    public DegreeAuditController(DegreeAuditService degreeAuditService) {
        this.degreeAuditService = degreeAuditService;
    }

    @GetMapping("/my-audit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DegreeAuditDto> getMyAudit(
            @RequestParam(required = false) String programCode,
            Principal principal) {
        return ResponseEntity.ok(
                degreeAuditService.evaluateStudentAudit(principal.getName(), programCode)
        );
    }

    @GetMapping("/student/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR')")
    public ResponseEntity<DegreeAuditDto> getStudentAudit(
            @PathVariable Long userId,
            @RequestParam(required = false) String programCode) {
        return ResponseEntity.ok(
                degreeAuditService.evaluateStudentAuditById(userId, programCode)
        );
    }

    @GetMapping("/requirements")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR', 'STUDENT')")
    public ResponseEntity<List<DegreeAuditItemDto>> getRequirements(
            @RequestParam(required = false) String programCode) {
        return ResponseEntity.ok(degreeAuditService.getProgramRequirements(programCode));
    }

    @PostMapping("/requirements")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<DegreeAuditItemDto> addRequirement(
            @Valid @RequestBody DegreeRequirementRequestDto request) {
        return ResponseEntity.ok(degreeAuditService.addRequirement(request));
    }

    @DeleteMapping("/requirements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<String> deleteRequirement(@PathVariable Long id) {
        degreeAuditService.deleteRequirement(id);
        return ResponseEntity.ok("Degree requirement removed.");
    }
}