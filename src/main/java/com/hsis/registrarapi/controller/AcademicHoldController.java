package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.AcademicHoldDto;
import com.hsis.registrarapi.dto.PlaceHoldRequestDto;
import com.hsis.registrarapi.dto.StudentLookupDto;
import com.hsis.registrarapi.dto.TranscriptDto;
import com.hsis.registrarapi.service.AcademicHoldService;
import com.hsis.registrarapi.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holds")
public class AcademicHoldController {

    private final AcademicHoldService academicHoldService;
    private final EnrollmentService enrollmentService;

    public AcademicHoldController(
            AcademicHoldService academicHoldService,
            EnrollmentService enrollmentService) {
        this.academicHoldService = academicHoldService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/my-holds")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AcademicHoldDto>> getMyHolds(Principal principal) {
        return ResponseEntity.ok(academicHoldService.getMyActiveHolds(principal.getName()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR')")
    public ResponseEntity<List<AcademicHoldDto>> getAllActiveHolds() {
        return ResponseEntity.ok(academicHoldService.getAllActiveHolds());
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR')")
    public ResponseEntity<List<StudentLookupDto>> searchStudents(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(academicHoldService.searchStudents(query));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR')")
    public ResponseEntity<List<AcademicHoldDto>> getStudentHolds(@PathVariable Long studentId) {
        return ResponseEntity.ok(academicHoldService.getStudentHolds(studentId));
    }

    @GetMapping("/student/{studentId}/transcript")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR')")
    public ResponseEntity<TranscriptDto> getStudentTranscript(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getStudentTranscriptByUserId(studentId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR')")
    public ResponseEntity<AcademicHoldDto> placeHold(
            @Valid @RequestBody PlaceHoldRequestDto request,
            Principal principal) {
        return ResponseEntity.ok(academicHoldService.placeHold(request, principal.getName()));
    }

    @PatchMapping("/{holdId}/lift")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR')")
    public ResponseEntity<AcademicHoldDto> liftHold(
            @PathVariable Long holdId,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal) {
        String note = body != null ? body.get("resolutionNote") : null;
        return ResponseEntity.ok(academicHoldService.liftHold(holdId, note, principal.getName()));
    }
}