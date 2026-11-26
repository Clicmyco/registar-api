package com.hsis.registrarapi.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class EnrollmentResponseDto {

    private Long enrollmentId;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private int credits;
    private String status;
    private LocalDateTime enrollmentDate;
    private String grade;
    private String advisorNote;
    private String termCode;
    private String termName;
    private String meetingDays;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;

    public EnrollmentResponseDto(
            Long enrollmentId,
            Long courseId,
            String courseCode,
            String courseTitle,
            int credits,
            String status,
            LocalDateTime enrollmentDate,
            String grade,
            String advisorNote,
            String termCode,
            String termName,
            String meetingDays,
            LocalTime startTime,
            LocalTime endTime,
            String room) {
        this.enrollmentId = enrollmentId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.credits = credits;
        this.status = status;
        this.enrollmentDate = enrollmentDate;
        this.grade = grade;
        this.advisorNote = advisorNote;
        this.termCode = termCode;
        this.termName = termName;
        this.meetingDays = meetingDays;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
    }

    public Long getEnrollmentId() { return enrollmentId; }
    public Long getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public int getCredits() { return credits; }
    public String getStatus() { return status; }
    public LocalDateTime getEnrollmentDate() { return enrollmentDate; }
    public String getGrade() { return grade; }
    public String getAdvisorNote() { return advisorNote; }
    public String getTermCode() { return termCode; }
    public String getTermName() { return termName; }
    public String getMeetingDays() { return meetingDays; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getRoom() { return room; }
}