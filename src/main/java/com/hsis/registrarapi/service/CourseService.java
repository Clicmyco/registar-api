package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.CourseDto;
import com.hsis.registrarapi.dto.CourseResponseDto;
import com.hsis.registrarapi.entity.AcademicTerm;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.repository.AcademicTermRepository;
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
    private final AcademicTermRepository academicTermRepository;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService,
            AcademicTermRepository academicTermRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
        this.academicTermRepository = academicTermRepository;
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

        // Attach specified term, or default to the currently active term
        AcademicTerm term = (courseData.getTermId() != null)
                ? academicTermRepository.findById(courseData.getTermId())
                        .orElseThrow(() -> new RuntimeException("Academic term not found."))
                : academicTermRepository.findByActiveTrue().orElse(null);
        newCourse.setTerm(term);

        Course saved = courseRepository.save(newCourse);

        String termNote = term != null ? " [" + term.getCode() + "]" : "";
        auditLogService.log(
                "COURSE_CREATED",
                "Course",
                "Created course " + saved.getCourseCode() + " (" + saved.getTitle() + ")" + termNote
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDto> getAllCourses(Long termId) {
        List<Course> courses = (termId != null)
                ? courseRepository.findByTermId(termId)
                : courseRepository.findAll();

        return courses.stream()
                .map(course -> {
                    AcademicTerm term = course.getTerm();
                    return new CourseResponseDto(
                            course.getId(),
                            course.getCourseCode(),
                            course.getTitle(),
                            course.getCredits(),
                            course.getMaxCapacity(),
                            enrollmentRepository.countByCourseIdAndStatus(course.getId(), "ENROLLED"),
                            course.getPrerequisite() != null ? course.getPrerequisite().getCourseCode() : null,
                            term != null ? term.getId() : null,
                            term != null ? term.getCode() : null,
                            term != null ? term.getName() : null
                    );
                })
                .collect(Collectors.toList());
    }
}