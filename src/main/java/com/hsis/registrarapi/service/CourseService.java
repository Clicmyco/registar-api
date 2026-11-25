package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.CourseDto;
import com.hsis.registrarapi.dto.CourseResponseDto;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final AuditLogService auditLogService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Course createCourse(CourseDto courseData) {
        Optional<Course> existingCourse = courseRepository.findByCourseCode(courseData.getCourseCode());
        if (existingCourse.isPresent()) {
            throw new RuntimeException("A course with this code already exists!");
        }

        Course newCourse = new Course();
        newCourse.setCourseCode(courseData.getCourseCode());
        newCourse.setTitle(courseData.getTitle());
        newCourse.setCredits(courseData.getCredits());
        newCourse.setMaxCapacity(courseData.getMaxCapacity());

        if (courseData.getPrerequisiteCourseId() != null) {
            Course prereq = courseRepository.findById(courseData.getPrerequisiteCourseId())
                    .orElseThrow(() -> new RuntimeException("Prerequisite course not found."));
            newCourse.setPrerequisite(prereq);
        }

        Course saved = courseRepository.save(newCourse);

        String prereqNote = saved.getPrerequisite() != null
                ? " [Prereq: " + saved.getPrerequisite().getCourseCode() + "]"
                : "";

        auditLogService.log(
                "COURSE_CREATED",
                "Course",
                "Created course " + saved.getCourseCode() + " (" + saved.getTitle() + ")" + prereqNote
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDto> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(course -> new CourseResponseDto(
                        course.getId(),
                        course.getCourseCode(),
                        course.getTitle(),
                        course.getCredits(),
                        course.getMaxCapacity(),
                        enrollmentRepository.countByCourseIdAndStatus(course.getId(), "ENROLLED"),
                        course.getPrerequisite() != null ? course.getPrerequisite().getCourseCode() : null
                ))
                .collect(Collectors.toList());
    }
}