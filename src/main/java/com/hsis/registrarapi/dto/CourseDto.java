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
}