package com.hsis.registrarapi.dto;

import java.time.LocalTime;

public class CourseResponseDto {

    private Long id;
    private String courseCode;
    private String title;
    private int credits;
    private int maxCapacity;
    private long enrolledCount;
    private String prerequisiteCourseCode;
    private Long termId;
    private String termCode;
    private String termName;
    private String meetingDays;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private long waitlistCount;
    private Long instructorId;
    private String instructorName;
    private String instructorEmail;

    public CourseResponseDto(
            Long id,
            String courseCode,
            String title,
            int credits,
            int maxCapacity,
            long enrolledCount,
            String prerequisiteCourseCode,
            Long termId,
            String termCode,
            String termName,
            String meetingDays,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            long waitlistCount) {
        this(id, courseCode, title, credits, maxCapacity, enrolledCount,
                prerequisiteCourseCode, termId, termCode, termName,
                meetingDays, startTime, endTime, room, waitlistCount,
                null, null, null);
    }

    public CourseResponseDto(
            Long id,
            String courseCode,
            String title,
            int credits,
            int maxCapacity,
            long enrolledCount,
            String prerequisiteCourseCode,
            Long termId,
            String termCode,
            String termName,
            String meetingDays,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            long waitlistCount,
            Long instructorId,
            String instructorName,
            String instructorEmail) {
        this.id = id;
        this.courseCode = courseCode;
        this.title = title;
        this.credits = credits;
        this.maxCapacity = maxCapacity;
        this.enrolledCount = enrolledCount;
        this.prerequisiteCourseCode = prerequisiteCourseCode;
        this.termId = termId;
        this.termCode = termCode;
        this.termName = termName;
        this.meetingDays = meetingDays;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.waitlistCount = waitlistCount;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.instructorEmail = instructorEmail;
    }

    public Long getId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public int getMaxCapacity() { return maxCapacity; }
    public long getEnrolledCount() { return enrolledCount; }
    public String getPrerequisiteCourseCode() { return prerequisiteCourseCode; }
    public Long getTermId() { return termId; }
    public String getTermCode() { return termCode; }
    public String getTermName() { return termName; }
    public String getMeetingDays() { return meetingDays; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getRoom() { return room; }
    public long getWaitlistCount() { return waitlistCount; }
    public Long getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public String getInstructorEmail() { return instructorEmail; }
}