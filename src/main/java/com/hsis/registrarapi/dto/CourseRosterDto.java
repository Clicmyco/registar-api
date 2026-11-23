package com.hsis.registrarapi.dto;

import java.time.LocalDateTime;

public class CourseRosterDto {

    private Long enrollmentId;
    private Long studentId;
    private String studentEmail;
    private String status;
    private LocalDateTime enrollmentDate;
    private String grade;

    public CourseRosterDto(Long enrollmentId, Long studentId, String studentEmail,
                           String status, LocalDateTime enrollmentDate ,String grade) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.status = status;
        this.enrollmentDate = enrollmentDate;
        this.grade=grade;
    }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Long getEnrollmentId() { return enrollmentId; }
    public Long getStudentId() { return studentId; }
    public String getStudentEmail() { return studentEmail; }
    public String getStatus() { return status; }
    public LocalDateTime getEnrollmentDate() { return enrollmentDate; }

    
}