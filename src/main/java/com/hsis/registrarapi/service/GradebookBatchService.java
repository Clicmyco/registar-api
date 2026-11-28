package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.BatchGradeRowResultDto;
import com.hsis.registrarapi.dto.BatchGradeUploadResponseDto;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.entity.Enrollment;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GradebookBatchService {

    private static final Set<String> VALID_GRADES = Set.of(
            "A+", "A", "A-",
            "B+", "B", "B-",
            "C+", "C", "C-",
            "D+", "D", "D-",
            "F", "P", "NP", "W", "I"
    );

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;

    public GradebookBatchService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
    }

    @Transactional(readOnly = true)
    public String exportCourseRosterCsv(Long courseId) {
        Course course = getCourseOrThrow(courseId);

        List<Enrollment> roster = enrollmentRepository.findByCourseId(course.getId()).stream()
                .filter(e -> "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .sorted(Comparator.comparing(e -> e.getUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("enrollmentId,studentEmail,studentName,status,grade\n");

        for (Enrollment e : roster) {
            csv.append(e.getId()).append(",")
               .append(escapeCsv(e.getUser().getEmail())).append(",")
               .append(escapeCsv(e.getUser().getFullName())).append(",")
               .append(escapeCsv(e.getStatus())).append(",")
               .append(escapeCsv(e.getGrade() != null ? e.getGrade() : ""))
               .append("\n");
        }

        return csv.toString();
    }

    @Transactional
    public BatchGradeUploadResponseDto processBatchGradeCsv(
            Long courseId,
            MultipartFile file,
            String actingUserEmail) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Please select a non-empty CSV file to upload.");
        }

        Course course = getCourseOrThrow(courseId);

        List<Enrollment> courseEnrollments = enrollmentRepository.findByCourseId(courseId);
        Map<Long, Enrollment> byEnrollmentId = new HashMap<>();
        Map<String, Enrollment> byStudentEmail = new HashMap<>();

        for (Enrollment e : courseEnrollments) {
            byEnrollmentId.put(e.getId(), e);
            byStudentEmail.put(e.getUser().getEmail().trim().toLowerCase(), e);
        }

        List<BatchGradeRowResultDto> results = new ArrayList<>();
        int updatedCount = 0;
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new RuntimeException("The uploaded CSV file is empty.");
            }

            // Strip UTF-8 BOM if exported from Excel
            headerLine = headerLine.replace("\uFEFF", "");
            List<String> headers = parseCsvLine(headerLine).stream()
                    .map(h -> h.trim().toLowerCase())
                    .toList();

            int idCol = headers.indexOf("enrollmentid");
            int emailCol = headers.indexOf("studentemail");
            int gradeCol = headers.indexOf("grade");

            if (gradeCol == -1 || (idCol == -1 && emailCol == -1)) {
                throw new RuntimeException(
                        "Invalid CSV header. Required columns: 'grade' and either 'enrollmentId' or 'studentEmail'."
                );
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> cols = parseCsvLine(line);
                String rawId = idCol >= 0 && idCol < cols.size() ? cols.get(idCol).trim() : "";
                String rawEmail = emailCol >= 0 && emailCol < cols.size() ? cols.get(emailCol).trim() : "";
                String rawGrade = gradeCol < cols.size() ? cols.get(gradeCol).trim().toUpperCase() : "";

                String identifier = !rawEmail.isEmpty() ? rawEmail : ("Enrollment #" + rawId);

                Enrollment target = null;
                if (!rawId.isEmpty()) {
                    try {
                        target = byEnrollmentId.get(Long.parseLong(rawId));
                    } catch (NumberFormatException ignored) {
                        // Fall back to email lookup if present
                    }
                }
                if (target == null && !rawEmail.isEmpty()) {
                    target = byStudentEmail.get(rawEmail.toLowerCase());
                }

                if (target == null) {
                    errorCount++;
                    results.add(new BatchGradeRowResultDto(
                            rowNumber,
                            identifier,
                            "—",
                            rawGrade,
                            "ERROR",
                            "Student enrollment not found in " + course.getCourseCode() + "."
                    ));
                    continue;
                }

                String studentName = target.getUser().getFullName();

                if (!"ENROLLED".equalsIgnoreCase(target.getStatus())) {
                    errorCount++;
                    results.add(new BatchGradeRowResultDto(
                            rowNumber,
                            target.getUser().getEmail(),
                            studentName,
                            rawGrade,
                            "ERROR",
                            "Cannot grade enrollment with status: " + target.getStatus()
                    ));
                    continue;
                }

                if (!rawGrade.isEmpty() && !VALID_GRADES.contains(rawGrade)) {
                    errorCount++;
                    results.add(new BatchGradeRowResultDto(
                            rowNumber,
                            target.getUser().getEmail(),
                            studentName,
                            rawGrade,
                            "ERROR",
                            "Invalid grade '" + rawGrade + "'. Allowed: A+, A, A-, B+, B, B-, C+, C, C-, D+, D, D-, F, P, NP, W, I."
                    ));
                    continue;
                }

                target.setGrade(rawGrade.isEmpty() ? null : rawGrade);
                enrollmentRepository.save(target);
                updatedCount++;

                results.add(new BatchGradeRowResultDto(
                        rowNumber,
                        target.getUser().getEmail(),
                        studentName,
                        rawGrade.isEmpty() ? "CLEARED" : rawGrade,
                        "UPDATED",
                        rawGrade.isEmpty() ? "Grade cleared." : "Grade saved."
                ));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read CSV file: " + ex.getMessage());
        }

        auditLogService.log(
                "BATCH_GRADES_UPLOADED",
                "Course",
                "Batch grade upload for " + course.getCourseCode() + " by " + actingUserEmail
                        + " (" + updatedCount + " updated, " + errorCount + " errors)"
        );

        return new BatchGradeUploadResponseDto(
                course.getId(),
                course.getCourseCode(),
                results.size(),
                updatedCount,
                errorCount,
                results
        );
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        tokens.add(current.toString());
        return tokens;
    }
}