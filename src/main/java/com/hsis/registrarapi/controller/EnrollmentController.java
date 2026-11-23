package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.CourseRosterDto;
import com.hsis.registrarapi.dto.EnrollmentRequestDto;
import com.hsis.registrarapi.dto.EnrollmentResponseDto;
import com.hsis.registrarapi.dto.GradeUpdateDto;
import com.hsis.registrarapi.service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
            enrollmentService.enrollStudent(principal.getName(), requestData.getCourseId());
            return ResponseEntity.ok("Successfully enrolled in the course!");
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

    // Only Admins can view the full student roster for a course
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseRoster(@PathVariable Long courseId) {
        try {
            List<CourseRosterDto> roster = enrollmentService.getCourseRoster(courseId);
            return ResponseEntity.ok(roster);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Only Admins can assign official grades
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{enrollmentId}/grade")
    public ResponseEntity<?> assignGrade(
            @PathVariable Long enrollmentId,
            @RequestBody GradeUpdateDto request) {
        try {
            enrollmentService.assignGrade(enrollmentId, request.getGrade());
            return ResponseEntity.ok("Grade updated successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}