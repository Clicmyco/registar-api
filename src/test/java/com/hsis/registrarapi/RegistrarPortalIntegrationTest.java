package com.hsis.registrarapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsis.registrarapi.dto.PlaceHoldRequestDto;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.entity.Enrollment;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import com.hsis.registrarapi.repository.UserRepository;
import com.hsis.registrarapi.service.EnrollmentService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegistrarPortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

   
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    private String loginAndGetToken(String email, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password
        ));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        if (body.has("token")) {
            return body.get("token").asText();
        }
        if (body.has("accessToken")) {
            return body.get("accessToken").asText();
        }
        fail("Login response did not contain a JWT token field: " + body);
        return null;
    }

    @Test
    @Order(1)
    void seededAccountsAuthenticateAndRespectRoleAccess() throws Exception {
        String studentToken = loginAndGetToken("student@university.edu", "Student123!");
        String advisorToken = loginAndGetToken("advisor@university.edu", "Advisor123!");

        // Student can query their own holds
        mockMvc.perform(get("/api/holds/my-holds")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        // Student cannot access staff campus-wide holds endpoint
        mockMvc.perform(get("/api/holds/active")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Advisor can access campus-wide active holds
        mockMvc.perform(get("/api/holds/active")
                        .header("Authorization", "Bearer " + advisorToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void academicHoldBlocksRegistrationUntilLifted() throws Exception {
        String advisorToken = loginAndGetToken("advisor@university.edu", "Advisor123!");
        User student = userRepository.findByEmail("student@university.edu").orElseThrow();
        Course cs101 = courseRepository.findAll().stream()
                .filter(c -> "CS101".equals(c.getCourseCode()))
                .findFirst()
                .orElseThrow();

        // 1. Advisor places an ADVISING hold on the student
        PlaceHoldRequestDto holdRequest = new PlaceHoldRequestDto(
                student.getId(),
                "ADVISING",
                "Mandatory freshman advising check-in"
        );

        MvcResult placeResult = mockMvc.perform(post("/api/holds")
                        .header("Authorization", "Bearer " + advisorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdType").value("ADVISING"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        Long holdId = objectMapper.readTree(placeResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 2. Student enrollment attempt must be blocked while hold is active
        RuntimeException blockedEx = assertThrows(
                RuntimeException.class,
                () -> enrollmentService.enrollStudent(student.getEmail(), cs101.getId())
        );
        assertTrue(blockedEx.getMessage().contains("Registration blocked by active ADVISING hold"));

        // 3. Advisor lifts the hold
        mockMvc.perform(patch("/api/holds/" + holdId + "/lift")
                        .header("Authorization", "Bearer " + advisorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resolutionNote", "Advising completed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // 4. Enrollment request now succeeds
        Enrollment enrollment = enrollmentService.enrollStudent(student.getEmail(), cs101.getId());
        assertEquals("PENDING_APPROVAL", enrollment.getStatus());
    }

    @Test
    @Order(3)
    void prerequisiteCheckBlocksUpperLevelCourseBeforePassingPrereq() {
        User student = userRepository.findByEmail("student@university.edu").orElseThrow();
        Course cs201 = courseRepository.findAll().stream()
                .filter(c -> "CS201".equals(c.getCourseCode()))
                .findFirst()
                .orElseThrow();

        RuntimeException prereqEx = assertThrows(
                RuntimeException.class,
                () -> enrollmentService.enrollStudent(student.getEmail(), cs201.getId())
        );
        assertTrue(prereqEx.getMessage().contains("Prerequisite required"));
    }

    @Test
    @Order(4)
    void instructorCanExportCsvRosterAndUploadBatchGrades() throws Exception {
        String instructorToken = loginAndGetToken("instructor@university.edu", "Instructor123!");
        User student = userRepository.findByEmail("student@university.edu").orElseThrow();
        Course cs101 = courseRepository.findAll().stream()
                .filter(c -> "CS101".equals(c.getCourseCode()))
                .findFirst()
                .orElseThrow();

        // Approve student's CS101 enrollment so they are actively ENROLLED
        Enrollment pending = enrollmentRepository.findByUserIdAndCourseId(student.getId(), cs101.getId())
                .orElseThrow();
        enrollmentService.reviewEnrollment(pending.getId(), true, "Approved for test", "advisor@university.edu");

        // 1. Export CSV roster as Instructor
        MvcResult exportResult = mockMvc.perform(get("/api/gradebook/courses/" + cs101.getId() + "/export-csv")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andReturn();

        String csvExport = exportResult.getResponse().getContentAsString();
        assertTrue(csvExport.contains("enrollmentId,studentEmail,studentName,status,grade"));
        assertTrue(csvExport.contains("student@university.edu"));

        // 2. Upload graded CSV back to batch-grades endpoint
        String gradedCsv = "enrollmentId,studentEmail,grade\n"
                + pending.getId() + ",student@university.edu,A\n";

        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "CS101_roster.csv",
                "text/csv",
                gradedCsv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/gradebook/courses/" + cs101.getId() + "/batch-grades")
                        .file(csvFile)
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(0));

        // 3. Verify final grade persisted on the enrollment record
        Enrollment graded = enrollmentRepository.findById(pending.getId()).orElseThrow();
        assertEquals("A", graded.getGrade());
    }
}