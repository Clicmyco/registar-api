package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.CourseDto;
import com.hsis.registrarapi.dto.CourseResponseDto;
import com.hsis.registrarapi.entity.AcademicTerm;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.AcademicTermRepository;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import com.hsis.registrarapi.repository.UserRepository;
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
    private final UserRepository userRepository;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService,
            AcademicTermRepository academicTermRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
        this.academicTermRepository = academicTermRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Course createCourse(CourseDto courseData) {
        Optional<Course> existingCourse = courseRepository.findByCourseCode(courseData.getCourseCode());
        if (existingCourse.isPresent()) {
            throw new RuntimeException("A course with this code already exists!");
        }

        if (courseData.getStartTime() != null && courseData.getEndTime() != null
                && !courseData.getStartTime().isBefore(courseData.getEndTime())) {
            throw new RuntimeException("Course end time must be after start time.");
        }

        Course newCourse = new Course();
        newCourse.setCourseCode(courseData.getCourseCode());
        newCourse.setTitle(courseData.getTitle());
        newCourse.setCredits(courseData.getCredits());
        newCourse.setMaxCapacity(courseData.getMaxCapacity());

        if (courseData.getMeetingDays() != null && !courseData.getMeetingDays().isBlank()) {
            newCourse.setMeetingDays(courseData.getMeetingDays().trim().toUpperCase());
        }
        newCourse.setStartTime(courseData.getStartTime());
        newCourse.setEndTime(courseData.getEndTime());
        if (courseData.getRoom() != null && !courseData.getRoom().isBlank()) {
            newCourse.setRoom(courseData.getRoom().trim());
        }

        if (courseData.getPrerequisiteCourseId() != null) {
            Course prereq = courseRepository.findById(courseData.getPrerequisiteCourseId())
                    .orElseThrow(() -> new RuntimeException("Prerequisite course not found."));
            newCourse.setPrerequisite(prereq);
        }

        AcademicTerm term = (courseData.getTermId() != null)
                ? academicTermRepository.findById(courseData.getTermId())
                        .orElseThrow(() -> new RuntimeException("Academic term not found."))
                : academicTermRepository.findByActiveTrue().orElse(null);
        newCourse.setTerm(term);

        if (courseData.getInstructorId() != null) {
            User instructor = userRepository.findById(courseData.getInstructorId())
                    .orElseThrow(() -> new RuntimeException("Assigned instructor not found."));
            newCourse.setInstructor(instructor);
        }

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
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDto> getCoursesByInstructor(String email) {
        return courseRepository.findByInstructorEmail(email).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseResponseDto assignInstructor(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found."));

        if (instructorId == null) {
            course.setInstructor(null);
            Course saved = courseRepository.save(course);
            auditLogService.log(
                    "INSTRUCTOR_UNASSIGNED",
                    "Course",
                    "Removed instructor from " + saved.getCourseCode()
            );
            return mapToResponseDto(saved);
        }

        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor user not found."));
        course.setInstructor(instructor);
        Course saved = courseRepository.save(course);

        auditLogService.log(
                "INSTRUCTOR_ASSIGNED",
                "Course",
                "Assigned " + instructor.getEmail() + " to teach " + saved.getCourseCode()
        );
        return mapToResponseDto(saved);
    }

    private CourseResponseDto mapToResponseDto(Course course) {
        AcademicTerm term = course.getTerm();
        User instructor = course.getInstructor();
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
                term != null ? term.getName() : null,
                course.getMeetingDays(),
                course.getStartTime(),
                course.getEndTime(),
                course.getRoom(),
                enrollmentRepository.countByCourseIdAndStatus(course.getId(), "WAITLISTED"),
                instructor != null ? instructor.getId() : null,
                instructor != null ? instructor.getFullName() : null,
                instructor != null ? instructor.getEmail() : null
        );
    }
}