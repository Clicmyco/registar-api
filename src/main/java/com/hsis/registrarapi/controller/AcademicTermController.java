package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.AcademicTermRequest;
import com.hsis.registrarapi.dto.TermStatusUpdateRequest;
import com.hsis.registrarapi.entity.AcademicTerm;
import com.hsis.registrarapi.service.AcademicTermService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
public class AcademicTermController {

    private final AcademicTermService termService;

    public AcademicTermController(AcademicTermService termService) {
        this.termService = termService;
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'ADVISOR', 'REGISTRAR', 'ADMIN')")
    @GetMapping("/active")
    public ResponseEntity<?> getActiveTerm() {
        try {
            return ResponseEntity.ok(termService.getActiveTerm());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('REGISTRAR', 'ADMIN','ADVISOR','STUDENT')")
    @GetMapping
    public ResponseEntity<List<AcademicTerm>> getAllTerms() {
        return ResponseEntity.ok(termService.getAllTerms());
    }

    @PreAuthorize("hasAnyRole('REGISTRAR', 'ADMIN')")
    @PostMapping
    public ResponseEntity<?> createTerm(@Valid @RequestBody AcademicTermRequest request) {
        try {
            return ResponseEntity.ok(termService.createTerm(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('REGISTRAR', 'ADMIN')")
    @PatchMapping("/{termId}")
    public ResponseEntity<?> updateTerm(
            @PathVariable Long termId,
            @RequestBody TermStatusUpdateRequest request) {
        try {
            return ResponseEntity.ok(termService.updateTermStatus(termId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}