package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.CourseRosterDto;
import com.hsis.registrarapi.dto.EnrollmentRequestDto;
import com.hsis.registrarapi.dto.EnrollmentResponseDto;
import com.hsis.registrarapi.dto.EnrollmentReviewRequest;
import com.hsis.registrarapi.dto.GradeUpdateDto;
import com.hsis.registrarapi.dto.PendingEnrollmentDto;
import com.hsis.registrarapi.dto.TranscriptDto;
import com.hsis.registrarapi.entity.Enrollment;
import com.hsis.registrarapi.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // Only students can enroll in classes
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<?> enrollInCourse(@RequestBody EnrollmentRequestDto requestData, Principal principal) {
        try {
            Enrollment saved = enrollmentService.enrollStudent(principal.getName(), requestData.getCourseId());
            if ("WAITLISTED".equalsIgnoreCase(saved.getStatus())) {
                return ResponseEntity.ok(
                        "Course is currently full. You have been added to the waitlist for "
                                + saved.getCourse().getCourseCode() + "."
                );
            }
            return ResponseEntity.ok("Enrollment request submitted for advisor approval.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Students can view only their own enrolled courses
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-courses")
    public ResponseEntity<List<EnrollmentResponseDto>> getMyEnrollments(Principal principal) {
        List<EnrollmentResponseDto> myEnrollments = enrollmentService.getStudentEnrollments(principal.getName());
        return ResponseEntity.ok(myEnrollments);
    }

    // Students can drop their own enrolled courses
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<?> dropCourse(@PathVariable Long enrollmentId, Principal principal) {
        try {
            enrollmentService.dropCourse(principal.getName(), enrollmentId);
            return ResponseEntity.ok("Successfully dropped the course!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Staff & assigned Instructors can view a course roster
    @PreAuthorize("hasAnyRole('ADMIN','REGISTRAR','ADVISOR','INSTRUCTOR')")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseRoster(@PathVariable Long courseId, Authentication authentication) {
        try {
            boolean hasFullRosterAccess = hasAnyAuthority(
                    authentication, "ROLE_ADMIN", "ROLE_REGISTRAR", "ROLE_ADVISOR"
            );
            List<CourseRosterDto> roster = enrollmentService.getCourseRosterForStaff(
                    courseId,
                    authentication.getName(),
                    hasFullRosterAccess
            );
            return ResponseEntity.ok(roster);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Admins, Registrars, and assigned Instructors can submit grades
    @PreAuthorize("hasAnyRole('ADMIN','REGISTRAR','INSTRUCTOR')")
    @PutMapping("/{enrollmentId}/grade")
    public ResponseEntity<?> assignGrade(
            @PathVariable Long enrollmentId,
            @RequestBody GradeUpdateDto request,
            Authentication authentication) {
        try {
            boolean isAdminOrRegistrar = hasAnyAuthority(
                    authentication, "ROLE_ADMIN", "ROLE_REGISTRAR"
            );
            enrollmentService.assignGradeForStaff(
                    enrollmentId,
                    request.getGrade(),
                    authentication.getName(),
                    isAdminOrRegistrar
            );
            return ResponseEntity.ok("Grade updated successfully.");
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Advisors, Registrars, and Admins view all pending enrollment requests
    @PreAuthorize("hasAnyRole('ADVISOR', 'REGISTRAR', 'ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<PendingEnrollmentDto>> getPendingEnrollments() {
        return ResponseEntity.ok(enrollmentService.getPendingEnrollments());
    }

    // Advisors, Registrars, and Admins approve or reject a pending request
    @PreAuthorize("hasAnyRole('ADVISOR', 'REGISTRAR', 'ADMIN')")
    @PatchMapping("/{enrollmentId}/review")
    public ResponseEntity<?> reviewEnrollment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody EnrollmentReviewRequest request,
            Principal principal) {
        try {
            enrollmentService.reviewEnrollment(
                    enrollmentId,
                    request.approve(),
                    request.advisorNote(),
                    principal.getName()
            );
            return ResponseEntity.ok(
                    request.approve()
                            ? "Enrollment request approved."
                            : "Enrollment request rejected."
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Students view their own official transcript
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/transcript")
    public ResponseEntity<TranscriptDto> getMyTranscript(Principal principal) {
        return ResponseEntity.ok(enrollmentService.getStudentTranscript(principal.getName()));
    }

    // Registrars and Admins can inspect any student's official transcript
    @PreAuthorize("hasAnyRole('REGISTRAR', 'ADMIN')")
    @GetMapping("/transcript/{userId}")
    public ResponseEntity<?> getStudentTranscript(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(enrollmentService.getStudentTranscriptByUserId(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    private boolean hasAnyAuthority(Authentication authentication, String... roles) {
        if (authentication == null) return false;
        List<String> targetRoles = Arrays.asList(roles);
        return authentication.getAuthorities().stream()
                .anyMatch(a -> targetRoles.contains(a.getAuthority()));
    }
}