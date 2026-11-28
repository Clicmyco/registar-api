package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.BatchGradeUploadResponseDto;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.service.GradebookBatchService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

@RestController
@RequestMapping("/api/gradebook")
@PreAuthorize("hasAnyRole('INSTRUCTOR', 'REGISTRAR', 'ADMIN')")
public class GradebookBatchController {

    private final GradebookBatchService gradebookBatchService;

    public GradebookBatchController(GradebookBatchService gradebookBatchService) {
        this.gradebookBatchService = gradebookBatchService;
    }

    @GetMapping("/courses/{courseId}/export-csv")
    public ResponseEntity<byte[]> exportRosterCsv(@PathVariable Long courseId) {
        Course course = gradebookBatchService.getCourseOrThrow(courseId);
        String csvContent = gradebookBatchService.exportCourseRosterCsv(courseId);
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        String filename = course.getCourseCode().replaceAll("[^a-zA-Z0-9_-]", "_") + "_roster.csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvBytes);
    }

    @PostMapping(value = "/courses/{courseId}/batch-grades", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchGradeUploadResponseDto> uploadBatchGrades(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        return ResponseEntity.ok(
                gradebookBatchService.processBatchGradeCsv(courseId, file, principal.getName())
        );
    }
}