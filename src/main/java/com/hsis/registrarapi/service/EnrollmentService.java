package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.CourseRosterDto;
import com.hsis.registrarapi.dto.EnrollmentResponseDto;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.entity.Enrollment;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import com.hsis.registrarapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private static final Set<String> VALID_GRADES = Set.of(
        "A", "A-", "B+", "B", "C+", "C", "D", "F"
    );

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

 
    @Transactional
    public Enrollment enrollStudent(String email, Long courseId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Optional<Enrollment> existingEnrollment =
                enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId());

        // 1. Block if already actively enrolled
        if (existingEnrollment.isPresent()
                && "ENROLLED".equalsIgnoreCase(existingEnrollment.get().getStatus())) {
            throw new RuntimeException("You are already enrolled in this course!");
        }

        // 2. Enforce seat capacity before enrolling or re-enrolling
        long activeCount = enrollmentRepository.countByCourseIdAndStatus(course.getId(), "ENROLLED");
        if (activeCount >= course.getMaxCapacity()) {
            throw new RuntimeException(
                    "This course is full (" + activeCount + "/" + course.getMaxCapacity() + " seats taken)."
            );
        }

        // 3. Re-activate if previously DROPPED
        if (existingEnrollment.isPresent()) {
            Enrollment dropped = existingEnrollment.get();
            dropped.setStatus("ENROLLED");
            dropped.setEnrollmentDate(LocalDateTime.now());
            return enrollmentRepository.save(dropped);
        }

        // 4. Otherwise, create a brand-new enrollment
        Enrollment newEnrollment = new Enrollment();
        newEnrollment.setUser(user);
        newEnrollment.setCourse(course);
        newEnrollment.setEnrollmentDate(LocalDateTime.now());
        newEnrollment.setStatus("ENROLLED");

        return enrollmentRepository.save(newEnrollment);
    }

    
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDto> getStudentEnrollments(String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Enrollment> enrollments = enrollmentRepository.findByUserId(student.getId());

        return enrollments.stream()
                .map(enrollment -> new EnrollmentResponseDto(
                        enrollment.getId(),
                        enrollment.getCourse().getId(),
                        enrollment.getCourse().getCourseCode(),
                        enrollment.getCourse().getTitle(),
                        enrollment.getCourse().getCredits(),
                        enrollment.getStatus(),
                        enrollment.getEnrollmentDate(),
                        enrollment.getGrade()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void dropCourse(String email, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        if (!enrollment.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to drop this enrollment!");
        }

        if ("DROPPED".equals(enrollment.getStatus())) {
            throw new RuntimeException("This course has already been dropped!");
        }

        enrollment.setStatus("DROPPED");
        enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<CourseRosterDto> getCourseRoster(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new RuntimeException("Course not found");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);

        return enrollments.stream()
                .map(enrollment -> new CourseRosterDto(
                        enrollment.getId(),
                        enrollment.getUser().getId(),
                        enrollment.getUser().getEmail(),
                        enrollment.getStatus(),
                        enrollment.getEnrollmentDate(),
                        enrollment.getGrade()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignGrade(Long enrollmentId, String rawGrade) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment record not found."));

        if (!"ENROLLED".equalsIgnoreCase(enrollment.getStatus())) {
            throw new RuntimeException("Cannot assign a grade to a dropped course.");
        }

        if (rawGrade == null || rawGrade.isBlank()) {
            enrollment.setGrade(null);
            enrollmentRepository.save(enrollment);
            return;
        }

        String normalized = rawGrade.trim().toUpperCase();
        if (!VALID_GRADES.contains(normalized)) {
            throw new RuntimeException("Invalid grade. Allowed values: A, A-, B+, B, C+, C, D, F.");
        }

        enrollment.setGrade(normalized);
        enrollmentRepository.save(enrollment);
    }
}