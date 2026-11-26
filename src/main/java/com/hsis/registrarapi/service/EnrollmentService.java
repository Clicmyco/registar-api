package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.CourseRosterDto;
import com.hsis.registrarapi.dto.EnrollmentResponseDto;
import com.hsis.registrarapi.dto.PendingEnrollmentDto;
import com.hsis.registrarapi.dto.TranscriptDto;
import com.hsis.registrarapi.entity.AcademicTerm;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.entity.Enrollment;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.AcademicTermRepository;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import com.hsis.registrarapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private static final Set<String> VALID_GRADES = Set.of(
            "A", "A-", "B+", "B", "C+", "C", "D", "F"
    );

    private static final Map<String, Double> GRADE_POINTS = Map.of(
            "A", 4.0,
            "A-", 3.75,
            "B+", 3.5,
            "B", 3.0,
            "C+", 2.5,
            "C", 2.0,
            "D", 1.0,
            "F", 0.0
    );

    private static final int MAX_SEMESTER_CREDITS = 19;

    private final AcademicTermRepository academicTermRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AuditLogService auditLogService;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            AuditLogService auditLogService,
            AcademicTermRepository academicTermRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.auditLogService = auditLogService;
        this.academicTermRepository = academicTermRepository;
    }

    @Transactional
    public Enrollment enrollStudent(String email, Long courseId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Optional<Enrollment> existingEnrollment =
                enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId());

        // 0. Check registration window for the course's term (or active term fallback)
        AcademicTerm targetTerm = course.getTerm() != null
                ? course.getTerm()
                : academicTermRepository.findByActiveTrue().orElse(null);

        if (targetTerm != null && !targetTerm.isRegistrationOpen()) {
            throw new RuntimeException(
                    "Registration is currently closed for " + targetTerm.getName() + "."
            );
        }

        // 1. Block if already ENROLLED or waiting on PENDING_APPROVAL
        if (existingEnrollment.isPresent()) {
            String currentStatus = existingEnrollment.get().getStatus();
            if ("ENROLLED".equalsIgnoreCase(currentStatus)) {
                throw new RuntimeException("You are already enrolled in this course!");
            }
            if ("PENDING_APPROVAL".equalsIgnoreCase(currentStatus)) {
                throw new RuntimeException("You already have a pending approval request for this course.");
            }
        }

        // 2a. Enforce 19-credit limit for the target semester
        Long targetTermId = targetTerm != null ? targetTerm.getId() : null;
        int currentCredits = enrollmentRepository.findByUserId(user.getId()).stream()
                .filter(e -> "ENROLLED".equalsIgnoreCase(e.getStatus())
                          || "PENDING_APPROVAL".equalsIgnoreCase(e.getStatus()))
                .filter(e -> {
                    if (targetTermId == null) return true;
                    AcademicTerm eTerm = e.getCourse().getTerm();
                    return eTerm == null || targetTermId.equals(eTerm.getId());
                })
                .mapToInt(e -> e.getCourse().getCredits())
                .sum();

        if (currentCredits + course.getCredits() > MAX_SEMESTER_CREDITS) {
            throw new RuntimeException(
                    "Credit limit exceeded: You currently have " + currentCredits
                    + " enrolled/pending credits"
                    + (targetTerm != null ? " in " + targetTerm.getName() : "")
                    + ". Adding " + course.getCourseCode() + " (" + course.getCredits()
                    + " Cr) would exceed the " + MAX_SEMESTER_CREDITS + "-credit limit."
            );
        }

        // 2b. Enforce course prerequisite (student must have a passing grade in the prerequisite)
        Course prerequisite = course.getPrerequisite();
        if (prerequisite != null) {
            boolean passedPrerequisite = enrollmentRepository
                    .findByUserIdAndCourseId(user.getId(), prerequisite.getId())
                    .map(e -> "ENROLLED".equalsIgnoreCase(e.getStatus())
                            && e.getGrade() != null
                            && !"F".equalsIgnoreCase(e.getGrade()))
                    .orElse(false);

            if (!passedPrerequisite) {
                throw new RuntimeException(
                        "Prerequisite required: You must complete and pass "
                        + prerequisite.getCourseCode() + " (" + prerequisite.getTitle()
                        + ") before requesting " + course.getCourseCode() + "."
                );
            }
        }

        // 2c. Check seat capacity before submitting a request
        long activeCount = enrollmentRepository.countByCourseIdAndStatus(course.getId(), "ENROLLED");
        if (activeCount >= course.getMaxCapacity()) {
            throw new RuntimeException(
                    "This course is full (" + activeCount + "/" + course.getMaxCapacity() + " seats taken)."
            );
        }

        // 3. Reuse record if previously DROPPED or REJECTED, otherwise create new
        Enrollment enrollment = existingEnrollment.orElseGet(Enrollment::new);
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setStatus("PENDING_APPROVAL");
        enrollment.setAdvisorNote(null);
        enrollment.setReviewedBy(null);
        enrollment.setReviewedAt(null);

        Enrollment saved = enrollmentRepository.save(enrollment);

        auditLogService.log(
                "ENROLLMENT_REQUESTED",
                "Enrollment",
                user.getEmail() + " requested enrollment in " + course.getCourseCode()
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<PendingEnrollmentDto> getPendingEnrollments() {
        return enrollmentRepository.findByStatusOrderByEnrollmentDateAsc("PENDING_APPROVAL")
                .stream()
                .map(enrollment -> {
                    Course course = enrollment.getCourse();
                    User student = enrollment.getUser();
                    long activeCount = enrollmentRepository.countByCourseIdAndStatus(course.getId(), "ENROLLED");

                    return new PendingEnrollmentDto(
                            enrollment.getId(),
                            student.getId(),
                            student.getFullName(),
                            student.getEmail(),
                            course.getId(),
                            course.getCourseCode(),
                            course.getTitle(),
                            course.getCredits(),
                            activeCount,
                            course.getMaxCapacity(),
                            enrollment.getEnrollmentDate()
                    );
                })
                .toList();
    }

    @Transactional
    public Enrollment reviewEnrollment(Long enrollmentId, boolean approve, String rawNote, String reviewerEmail) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment request not found."));

        if (!"PENDING_APPROVAL".equalsIgnoreCase(enrollment.getStatus())) {
            throw new RuntimeException("Only PENDING_APPROVAL requests can be reviewed.");
        }

        String cleanedNote = (rawNote == null || rawNote.isBlank()) ? null : rawNote.trim();

        if (approve) {
            Course course = enrollment.getCourse();
            long activeCount = enrollmentRepository.countByCourseIdAndStatus(course.getId(), "ENROLLED");
            if (activeCount >= course.getMaxCapacity()) {
                throw new RuntimeException(
                        "Cannot approve: " + course.getCourseCode() + " is now full ("
                                + activeCount + "/" + course.getMaxCapacity() + ")."
                );
            }

            enrollment.setStatus("ENROLLED");
            enrollment.setAdvisorNote(cleanedNote);
            enrollment.setReviewedBy(reviewerEmail);
            enrollment.setReviewedAt(LocalDateTime.now());
            Enrollment saved = enrollmentRepository.save(enrollment);

            auditLogService.log(
                    "ENROLLMENT_APPROVED",
                    "Enrollment",
                    "Approved " + enrollment.getUser().getEmail() + " for " + course.getCourseCode()
            );
            return saved;
        } else {
            if (cleanedNote == null) {
                throw new RuntimeException("Please provide a brief reason when rejecting an enrollment request.");
            }

            enrollment.setStatus("REJECTED");
            enrollment.setAdvisorNote(cleanedNote);
            enrollment.setReviewedBy(reviewerEmail);
            enrollment.setReviewedAt(LocalDateTime.now());
            Enrollment saved = enrollmentRepository.save(enrollment);

            auditLogService.log(
                    "ENROLLMENT_REJECTED",
                    "Enrollment",
                    "Rejected " + enrollment.getUser().getEmail() + " for "
                            + enrollment.getCourse().getCourseCode() + " (" + cleanedNote + ")"
            );
            return saved;
        }
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDto> getStudentEnrollments(String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return enrollmentRepository.findByUserId(student.getId()).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void dropCourse(String email, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        if (!enrollment.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to drop this enrollment!");
        }

        if ("DROPPED".equalsIgnoreCase(enrollment.getStatus())
                || "REJECTED".equalsIgnoreCase(enrollment.getStatus())) {
            throw new RuntimeException("This course cannot be dropped in its current state.");
        }

        if ("ENROLLED".equalsIgnoreCase(enrollment.getStatus())) {
            AcademicTerm courseTerm = enrollment.getCourse().getTerm() != null
                    ? enrollment.getCourse().getTerm()
                    : academicTermRepository.findByActiveTrue().orElse(null);

            if (courseTerm != null
                    && courseTerm.getAddDropDeadline() != null
                    && LocalDate.now().isAfter(courseTerm.getAddDropDeadline())) {
                throw new RuntimeException(
                        "The add/drop deadline (" + courseTerm.getAddDropDeadline()
                        + ") for " + courseTerm.getName() + " has passed."
                );
            }
        }

        enrollment.setStatus("DROPPED");
        enrollmentRepository.save(enrollment);

        auditLogService.log(
                "COURSE_DROPPED",
                "Enrollment",
                email + " dropped " + enrollment.getCourse().getCourseCode()
        );
    }

    @Transactional(readOnly = true)
    public List<CourseRosterDto> getCourseRoster(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new RuntimeException("Course not found");
        }

        return enrollmentRepository.findByCourseId(courseId).stream()
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
            throw new RuntimeException("Cannot assign a grade unless the student is actively ENROLLED.");
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

        auditLogService.log(
                "GRADE_ASSIGNED",
                "Enrollment",
                "Assigned grade " + normalized + " to " + enrollment.getUser().getEmail()
                        + " in " + enrollment.getCourse().getCourseCode()
        );
    }

    @Transactional(readOnly = true)
    public TranscriptDto getStudentTranscript(String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return buildTranscript(student);
    }

    @Transactional(readOnly = true)
    public TranscriptDto getStudentTranscriptByUserId(Long userId) {
        User student = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return buildTranscript(student);
    }

    private TranscriptDto buildTranscript(User student) {
        List<EnrollmentResponseDto> records = enrollmentRepository.findByUserId(student.getId()).stream()
                .filter(e -> "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .map(this::mapToResponseDto)
                .toList();

        int attemptedCredits = 0;
        int earnedCredits = 0;
        double totalQualityPoints = 0.0;

        for (EnrollmentResponseDto item : records) {
            String grade = item.getGrade();
            if (grade != null && GRADE_POINTS.containsKey(grade)) {
                int credits = item.getCredits();
                double gp = GRADE_POINTS.get(grade);
                attemptedCredits += credits;
                totalQualityPoints += gp * credits;
                if (!"F".equalsIgnoreCase(grade)) {
                    earnedCredits += credits;
                }
            }
        }

        double cumulativeGpa = attemptedCredits > 0
                ? Math.round((totalQualityPoints / attemptedCredits) * 100.0) / 100.0
                : 0.0;

        String standing;
        if (attemptedCredits == 0) {
            standing = "In Progress (No Graded Credits)";
        } else if (cumulativeGpa >= 3.50 && earnedCredits >= 12) {
            standing = "Dean's List";
        } else if (cumulativeGpa >= 2.00) {
            standing = "Good Standing";
        } else {
            standing = "Academic Probation";
        }

        return new TranscriptDto(
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                attemptedCredits,
                earnedCredits,
                Math.round(totalQualityPoints * 100.0) / 100.0,
                cumulativeGpa,
                standing,
                LocalDateTime.now(),
                records
        );
    }

    private EnrollmentResponseDto mapToResponseDto(Enrollment enrollment) {
        AcademicTerm term = enrollment.getCourse().getTerm();
        return new EnrollmentResponseDto(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getCourseCode(),
                enrollment.getCourse().getTitle(),
                enrollment.getCourse().getCredits(),
                enrollment.getStatus(),
                enrollment.getEnrollmentDate(),
                enrollment.getGrade(),
                enrollment.getAdvisorNote(),
                term != null ? term.getCode() : null,
                term != null ? term.getName() : null
        );
    }
}