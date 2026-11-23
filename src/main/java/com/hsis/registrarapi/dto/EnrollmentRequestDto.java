package com.hsis.registrarapi.dto;

public class EnrollmentRequestDto {
    private Long courseId;
    private String grade;
    public EnrollmentRequestDto() {}

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    
}
