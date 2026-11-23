package com.hsis.registrarapi.dto;

public class CourseDto {

    private String courseCode;
    private String title;
    private int credits;

    public CourseDto() {}
private Integer maxCapacity;

public Integer getMaxCapacity() { return maxCapacity; }
public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    // Getters and Setters
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
}