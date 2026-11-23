package com.hsis.registrarapi.dto;

import java.time.LocalDateTime;

public class EnrollmentResponseDto {

    private Long enrollmentId;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private int credits;
    private String status;
    private LocalDateTime enrollmentDate;
    private String grade;
    // Constructor to populate all fields at once
    public EnrollmentResponseDto(Long enrollmentId, Long courseId, String courseCode,
                                 String courseTitle, int credits, String status,
                                 LocalDateTime enrollmentDate ,String grade) {
        this.enrollmentId = enrollmentId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.credits = credits;
        this.status = status;
        this.enrollmentDate = enrollmentDate;
        this.grade=grade;
    }

    // Getters (Spring uses these to convert the object into JSON)
    public Long getEnrollmentId() { return enrollmentId; }
    public Long getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public int getCredits() { return credits; }
    public String getStatus() { return status; }
    public LocalDateTime getEnrollmentDate() { return enrollmentDate; }
    public String getGrade() { return grade; }

    
}