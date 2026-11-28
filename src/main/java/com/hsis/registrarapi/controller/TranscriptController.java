package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.service.TranscriptPdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/transcripts")
public class TranscriptController {

    private final TranscriptPdfService transcriptPdfService;

    public TranscriptController(TranscriptPdfService transcriptPdfService) {
        this.transcriptPdfService = transcriptPdfService;
    }

    @GetMapping("/my-pdf")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> downloadMyTranscriptPdf(Principal principal) {
        byte[] pdfBytes = transcriptPdfService.generateTranscriptPdfForEmail(principal.getName());
        return buildPdfResponse(pdfBytes, "Official_Transcript.pdf");
    }

    @GetMapping("/student/{studentId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ADVISOR')")
    public ResponseEntity<byte[]> downloadStudentTranscriptPdf(@PathVariable Long studentId) {
        byte[] pdfBytes = transcriptPdfService.generateTranscriptPdfForStudentId(studentId);
        return buildPdfResponse(pdfBytes, "Official_Transcript_Student_" + studentId + ".pdf");
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
        );
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}