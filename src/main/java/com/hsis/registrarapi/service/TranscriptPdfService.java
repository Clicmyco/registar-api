package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.EnrollmentResponseDto;
import com.hsis.registrarapi.dto.TranscriptDto;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TranscriptPdfService {

    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;

    public TranscriptPdfService(
            UserRepository userRepository,
            EnrollmentService enrollmentService) {
        this.userRepository = userRepository;
        this.enrollmentService = enrollmentService;
    }

    public byte[] generateTranscriptPdfForEmail(String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student account not found."));
        TranscriptDto transcript = enrollmentService.getStudentTranscriptByUserId(student.getId());
        return buildPdfBytes(transcript);
    }

    public byte[] generateTranscriptPdfForStudentId(Long studentId) {
        TranscriptDto transcript = enrollmentService.getStudentTranscriptByUserId(studentId);
        return buildPdfBytes(transcript);
    }

    private byte[] buildPdfBytes(TranscriptDto transcript) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            // Color Palette
            Color indigoHeader = new Color(30, 41, 59);     // Slate-800
            Color accentIndigo = new Color(67, 56, 202);    // Indigo-700
            Color lightBg = new Color(248, 250, 252);       // Slate-50
            Color borderGray = new Color(203, 213, 225);    // Slate-300
            Color mutedText = new Color(100, 116, 139);     // Slate-500

            // Fonts
            Font uniTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
            Font docSubtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(226, 232, 240));
            Font sectionHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, indigoHeader);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, mutedText);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, indigoHeader);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font cellBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, indigoHeader);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, mutedText);

            // 1. University Header Banner
            PdfPTable bannerTable = new PdfPTable(1);
            bannerTable.setWidthPercentage(100);
            PdfPCell bannerCell = new PdfPCell();
            bannerCell.setBackgroundColor(indigoHeader);
            bannerCell.setPadding(16);
            bannerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph uniName = new Paragraph("HSIS UNIVERSITY — OFFICE OF THE REGISTRAR", uniTitleFont);
            uniName.setSpacingAfter(4);
            bannerCell.addElement(uniName);

            LocalDateTime timestamp = transcript.generatedAt() != null ? transcript.generatedAt() : LocalDateTime.now();
            String issuedDate = timestamp.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm"));
            Paragraph sub = new Paragraph("OFFICIAL ACADEMIC TRANSCRIPT  |  Issued: " + issuedDate, docSubtitleFont);
            bannerCell.addElement(sub);

            bannerTable.addCell(bannerCell);
            bannerTable.setSpacingAfter(16);
            document.add(bannerTable);

            // 2. Student Identity & Academic Standing Summary Box
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1.2f, 1f});
            infoTable.setSpacingAfter(16);

            addSummaryCell(
                    infoTable,
                    "STUDENT NAME",
                    transcript.studentName() != null ? transcript.studentName() : "—",
                    labelFont,
                    valueFont,
                    lightBg,
                    borderGray
            );
            addSummaryCell(
                    infoTable,
                    "CUMULATIVE GPA",
                    String.format("%.2f / 4.00", transcript.cumulativeGpa()),
                    labelFont,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, accentIndigo),
                    lightBg,
                    borderGray
            );

            addSummaryCell(
                    infoTable,
                    "STUDENT EMAIL / ID",
                    transcript.studentEmail() + " (ID #" + transcript.studentId() + ")",
                    labelFont,
                    valueFont,
                    lightBg,
                    borderGray
            );
            addSummaryCell(
                    infoTable,
                    "ACADEMIC STANDING",
                    transcript.academicStanding() != null ? transcript.academicStanding() : "Good Standing",
                    labelFont,
                    valueFont,
                    lightBg,
                    borderGray
            );

            String creditsText = transcript.earnedCredits() + " Earned / " + transcript.attemptedCredits() + " Attempted";
            addSummaryCell(infoTable, "CREDIT HOURS", creditsText, labelFont, valueFont, lightBg, borderGray);

            String qpText = String.format("%.2f", transcript.totalQualityPoints());
            addSummaryCell(infoTable, "TOTAL QUALITY POINTS", qpText, labelFont, valueFont, lightBg, borderGray);

            document.add(infoTable);

            // 3. Coursework Record Table
            Paragraph recordHeading = new Paragraph("ACADEMIC COURSEWORK RECORD", sectionHeadingFont);
            recordHeading.setSpacingAfter(8);
            document.add(recordHeading);

            PdfPTable courseTable = new PdfPTable(5);
            courseTable.setWidthPercentage(100);
            courseTable.setWidths(new float[]{1.1f, 1.2f, 3.3f, 0.9f, 1.1f});
            courseTable.setHeaderRows(1);

            String[] headers = {"TERM", "COURSE", "COURSE TITLE", "CREDITS", "GRADE"};
            for (int i = 0; i < headers.length; i++) {
                PdfPCell hCell = new PdfPCell(new Phrase(headers[i], tableHeaderFont));
                hCell.setBackgroundColor(accentIndigo);
                hCell.setPadding(8);
                hCell.setBorderColor(accentIndigo);
                hCell.setHorizontalAlignment(i >= 3 ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
                courseTable.addCell(hCell);
            }

            if (transcript.records() == null || transcript.records().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(
                        new Phrase("No enrolled or graded coursework on record for this student.", cellFont)
                );
                emptyCell.setColspan(5);
                emptyCell.setPadding(14);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setBorderColor(borderGray);
                courseTable.addCell(emptyCell);
            } else {
                boolean alternate = false;
                for (EnrollmentResponseDto rec : transcript.records()) {
                    Color rowBg = alternate ? lightBg : Color.WHITE;
                    alternate = !alternate;

                    addCourseCell(courseTable, rec.getTermCode() != null ? rec.getTermCode() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                    addCourseCell(courseTable, rec.getCourseCode() != null ? rec.getCourseCode() : "—", cellBoldFont, rowBg, borderGray, Element.ALIGN_LEFT);
                    addCourseCell(courseTable, rec.getCourseTitle() != null ? rec.getCourseTitle() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                    addCourseCell(courseTable, String.valueOf(rec.getCredits()), cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                    String gradeDisplay = (rec.getGrade() != null && !rec.getGrade().isBlank())
                            ? rec.getGrade()
                            : "In Progress";
                    addCourseCell(courseTable, gradeDisplay, cellBoldFont, rowBg, borderGray, Element.ALIGN_CENTER);
                }
            }

            courseTable.setSpacingAfter(20);
            document.add(courseTable);

            // 4. Official Verification Footer
            Paragraph footer = new Paragraph(
                    "END OF OFFICIAL TRANSCRIPT  •  Generated electronically by the HSIS University Registrar Portal.",
                    footerFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate official PDF transcript: " + e.getMessage(), e);
        }
    }

    private void addSummaryCell(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont,
            Color bg,
            Color border) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setPadding(8);

        Paragraph pLabel = new Paragraph(label, labelFont);
        pLabel.setSpacingAfter(2);
        cell.addElement(pLabel);

        Paragraph pVal = new Paragraph(value, valueFont);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private void addCourseCell(
            PdfPTable table,
            String text,
            Font font,
            Color bg,
            Color border,
            int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setPadding(7);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }
}