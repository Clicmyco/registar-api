package com.hsis.registrarapi.dto;
import jakarta.validation.constraints.*;;
public class CourseDto {

    @NotBlank(message = "Course code is required")
@Size(min = 3, max = 15, message = "Course code must be 3 to 15 characters")
private String courseCode;

@NotBlank(message = "Course title is required")
private String title;

@Min(value = 1, message = "Credits must be at least 1")
@Max(value = 6, message = "Credits cannot exceed 6")
private int credits;

@Min(value = 1, message = "Seat capacity must be at least 1")
@Max(value = 500, message = "Seat capacity cannot exceed 500")
private int maxCapacity;


    public CourseDto() {}

    private Long prerequisiteCourseId;

  private String meetingDays;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
    private String room;

    public String getMeetingDays() { return meetingDays; }
    public void setMeetingDays(String meetingDays) { this.meetingDays = meetingDays; }

    public java.time.LocalTime getStartTime() { return startTime; }
    public void setStartTime(java.time.LocalTime startTime) { this.startTime = startTime; }

    public java.time.LocalTime getEndTime() { return endTime; }
    public void setEndTime(java.time.LocalTime endTime) { this.endTime = endTime; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }


public Integer getMaxCapacity() { return maxCapacity; }
public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    // Getters and Setters
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    
    public Long getPrerequisiteCourseId() { return prerequisiteCourseId; }
    public void setPrerequisiteCourseId(Long prerequisiteCourseId) { this.prerequisiteCourseId = prerequisiteCourseId; }

    private Long termId;

   public Long getTermId() { return termId; }
   public void setTermId(Long termId) { this.termId = termId; }
}