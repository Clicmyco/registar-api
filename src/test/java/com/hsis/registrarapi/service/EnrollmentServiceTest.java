package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.TranscriptDto;
import com.hsis.registrarapi.entity.AcademicTerm;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.entity.Enrollment;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.AcademicTermRepository;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import com.hsis.registrarapi.repository.UserRepository;

import org.checkerframework.checker.units.qual.m;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AcademicTermRepository academicTermRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User student;
    private AcademicTerm activeTerm;
    private Course cs101;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setEmail("student@university.edu");
        student.setFirstName("Selam");
        student.setMiddleName(null);
        student.setLastName(null);

        activeTerm = new AcademicTerm();
        activeTerm.setId(10L);
        activeTerm.setCode("2026-FALL");
        activeTerm.setName("Fall Semester 2026");
        activeTerm.setRegistrationOpen(true);
        activeTerm.setAddDropDeadline(LocalDate.now().plusDays(14));

        cs101 = new Course();
        cs101.setId(100L);
        cs101.setCourseCode("CS101");
        cs101.setTitle("Intro to Computer Science");
        cs101.setCredits(3);
        cs101.setMaxCapacity(30);
        cs101.setTerm(activeTerm);
    }

    @Test
    void enrollStudent_SubmitsPendingApprovalWhenSeatsAvailable() {
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseRepository.findById(cs101.getId())).thenReturn(Optional.of(cs101));
        when(enrollmentRepository.findByUserIdAndCourseId(student.getId(), cs101.getId()))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByUserId(student.getId())).thenReturn(List.of());
        when(enrollmentRepository.countByCourseIdAndStatus(cs101.getId(), "ENROLLED")).thenReturn(5L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment result = enrollmentService.enrollStudent(student.getEmail(), cs101.getId());

        assertEquals("PENDING_APPROVAL", result.getStatus());
        verify(auditLogService).log(eq("ENROLLMENT_REQUESTED"), eq("Enrollment"), anyString());
    }

    @Test
    void enrollStudent_PlacesOnWaitlistWhenCourseIsFull() {
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseRepository.findById(cs101.getId())).thenReturn(Optional.of(cs101));
        when(enrollmentRepository.findByUserIdAndCourseId(student.getId(), cs101.getId()))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByUserId(student.getId())).thenReturn(List.of());
        when(enrollmentRepository.countByCourseIdAndStatus(cs101.getId(), "ENROLLED")).thenReturn(30L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment result = enrollmentService.enrollStudent(student.getEmail(), cs101.getId());

        assertEquals("WAITLISTED", result.getStatus());
        verify(auditLogService).log(eq("ENROLLMENT_WAITLISTED"), eq("Enrollment"), anyString());
    }

    @Test
    void enrollStudent_ThrowsWhenRegistrationWindowClosed() {
        activeTerm.setRegistrationOpen(false);

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseRepository.findById(cs101.getId())).thenReturn(Optional.of(cs101));
        when(enrollmentRepository.findByUserIdAndCourseId(student.getId(), cs101.getId()))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> enrollmentService.enrollStudent(student.getEmail(), cs101.getId())
        );

        assertTrue(ex.getMessage().contains("Registration is currently closed"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollStudent_ThrowsWhen19CreditLimitExceeded() {
        Course heavyLoad = new Course();
        heavyLoad.setId(99L);
        heavyLoad.setCredits(17);
        heavyLoad.setTerm(activeTerm);

        Enrollment existing = new Enrollment();
        existing.setCourse(heavyLoad);
        existing.setStatus("ENROLLED");

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseRepository.findById(cs101.getId())).thenReturn(Optional.of(cs101));
        when(enrollmentRepository.findByUserIdAndCourseId(student.getId(), cs101.getId()))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByUserId(student.getId())).thenReturn(List.of(existing));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> enrollmentService.enrollStudent(student.getEmail(), cs101.getId())
        );

        assertTrue(ex.getMessage().contains("Credit limit exceeded"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollStudent_ThrowsWhenPrerequisiteFailedOrMissing() {
        Course cs201 = new Course();
        cs201.setId(200L);
        cs201.setCourseCode("CS201");
        cs201.setTitle("Data Structures");
        cs201.setCredits(3);
        cs201.setMaxCapacity(30);
        cs201.setTerm(activeTerm);
        cs201.setPrerequisite(cs101);

        Enrollment failedPrereq = new Enrollment();
        failedPrereq.setCourse(cs101);
        failedPrereq.setStatus("ENROLLED");
        failedPrereq.setGrade("F");

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseRepository.findById(cs201.getId())).thenReturn(Optional.of(cs201));
        when(enrollmentRepository.findByUserIdAndCourseId(student.getId(), cs201.getId()))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByUserId(student.getId())).thenReturn(List.of());
        when(enrollmentRepository.findByUserIdAndCourseId(student.getId(), cs101.getId()))
                .thenReturn(Optional.of(failedPrereq));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> enrollmentService.enrollStudent(student.getEmail(), cs201.getId())
        );

        assertTrue(ex.getMessage().contains("Prerequisite required"));
    }

    @Test
    void enrollStudent_ThrowsOnOverlappingScheduleInSameTerm() {
        Course existingCourse = new Course();
        existingCourse.setId(101L);
        existingCourse.setCourseCode("MATH101");
        existingCourse.setCredits(3);
        existingCourse.setTerm(activeTerm);
        existingCourse.setMeetingDays("MON,WED");
        existingCourse.setStartTime(LocalTime.of(9, 0));
        existingCourse.setEndTime(LocalTime.of(10, 30));

        Enrollment enrolledMath = new Enrollment();
        enrolledMath.setCourse(existingCourse);
        enrolledMath.setStatus("ENROLLED");

        cs101.setMeetingDays("WED,FRI");
        cs101.setStartTime(LocalTime.of(10, 0));
        cs101.setEndTime(LocalTime.of(11, 30));

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseRepository.findById(cs101.getId())).thenReturn(Optional.of(cs101));
        when(enrollmentRepository.findByUserIdAndCourseId(student.getId(), cs101.getId()))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByUserId(student.getId())).thenReturn(List.of(enrolledMath));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> enrollmentService.enrollStudent(student.getEmail(), cs101.getId())
        );

        assertTrue(ex.getMessage().contains("Schedule conflict"));
    }

    @Test
    void dropCourse_AutoPromotesEarliestWaitlistedStudent() {
        cs101.setMaxCapacity(1);

        Enrollment activeEnrollment = new Enrollment();
        activeEnrollment.setId(501L);
        activeEnrollment.setUser(student);
        activeEnrollment.setCourse(cs101);
        activeEnrollment.setStatus("ENROLLED");

        User waitlistedStudent = new User();
        waitlistedStudent.setId(2L);
        waitlistedStudent.setEmail("next@university.edu");

        Enrollment waitlistedEnrollment = new Enrollment();
        waitlistedEnrollment.setId(502L);
        waitlistedEnrollment.setUser(waitlistedStudent);
        waitlistedEnrollment.setCourse(cs101);
        waitlistedEnrollment.setStatus("WAITLISTED");

        when(enrollmentRepository.findById(501L)).thenReturn(Optional.of(activeEnrollment));
        when(enrollmentRepository.countByCourseIdAndStatus(cs101.getId(), "ENROLLED")).thenReturn(0L);
        when(enrollmentRepository.findByCourseIdAndStatusOrderByEnrollmentDateAsc(cs101.getId(), "WAITLISTED"))
                .thenReturn(List.of(waitlistedEnrollment));

        enrollmentService.dropCourse(student.getEmail(), 501L);

        assertEquals("DROPPED", activeEnrollment.getStatus());
        assertEquals("PENDING_APPROVAL", waitlistedEnrollment.getStatus());
        assertEquals("Auto-promoted from waitlist", waitlistedEnrollment.getAdvisorNote());
        verify(enrollmentRepository, times(2)).save(any(Enrollment.class));
    }

    @Test
    void getStudentTranscript_ComputesDeansListGpaAndStanding() {
        String[] grades = {"A", "A", "A-", "B+"}; // (4.0 + 4.0 + 3.75 + 3.5) / 4 = 3.8125 -> 3.81
        List<Enrollment> graded = java.util.stream.IntStream.range(0, grades.length)
                .mapToObj(i -> {
                    Course c = new Course();
                    c.setId((long) (300 + i));
                    c.setCourseCode("CS30" + i);
                    c.setTitle("Course " + i);
                    c.setCredits(3);
                    c.setTerm(activeTerm);

                    Enrollment e = new Enrollment();
                    e.setId((long) (600 + i));
                    e.setCourse(c);
                    e.setStatus("ENROLLED");
                    e.setGrade(grades[i]);
                    return e;
                })
                .toList();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserId(student.getId())).thenReturn(graded);

        TranscriptDto transcript = enrollmentService.getStudentTranscript(student.getEmail());

        assertEquals(12, transcript.attemptedCredits());
        assertEquals(12, transcript.earnedCredits());
        assertEquals(3.81, transcript.cumulativeGpa());
        assertEquals("Dean's List", transcript.academicStanding());
    }
}