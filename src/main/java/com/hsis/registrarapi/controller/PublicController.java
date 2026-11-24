package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.entity.AcademicProgram;
import com.hsis.registrarapi.repository.AcademicProgramRepository;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final AcademicProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public record PublicStatsDto(long totalPrograms, long totalCourses, long totalEnrollments) {}

    public PublicController(
            AcademicProgramRepository programRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository) {
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<PublicStatsDto> getPublicStats() {
        PublicStatsDto stats = new PublicStatsDto(
                programRepository.count(),
                courseRepository.count(),
                enrollmentRepository.count()
        );
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/programs")
    public ResponseEntity<List<AcademicProgram>> getPrograms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String degreeType) {

        String query = (search == null) ? "" : search.trim().toLowerCase();
        String typeFilter = (degreeType == null) ? "" : degreeType.trim().toLowerCase();

        List<AcademicProgram> results = programRepository.findAll().stream()
                .filter(p -> query.isEmpty()
                        || p.getName().toLowerCase().contains(query)
                        || p.getProgramCode().toLowerCase().contains(query)
                        || p.getDepartment().toLowerCase().contains(query))
                .filter(p -> typeFilter.isEmpty()
                        || typeFilter.equals("all")
                        || p.getDegreeType().toLowerCase().equals(typeFilter))
                .toList();

        return ResponseEntity.ok(results);
    }
}