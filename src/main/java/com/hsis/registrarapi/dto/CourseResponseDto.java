package com.hsis.registrarapi.dto;

public class CourseResponseDto {
    private Long id;
    private String courseCode;
    private String title;
    private int credits;
    private int maxCapacity;
    private Long enrolledCount;
    private String prerequisiteCoursecode;
    private Long termId;
    private  String termCode;
     private String termName;

    public CourseResponseDto(
            Long id,
            String courseCode,
            String title,
            int credits,
            int maxCapacity,
            long enrolledCount,
            String prerequisiteCoursecode,
            Long termId,
            String termCode,
            String termName
        )
             {
        this.id = id;
        this.courseCode = courseCode;
        this.title = title;
        this.credits = credits;
        this.maxCapacity = maxCapacity;
        this.enrolledCount = enrolledCount;
        this.prerequisiteCoursecode=prerequisiteCoursecode;
        this.termId=termId;
        this.termCode=termCode;
        this.termName=termName;
    }

    public Long getId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public int getMaxCapacity() { return maxCapacity; }
    public long getEnrolledCount() { return enrolledCount; }
    public String getPrerequisiteCoursecode(){return prerequisiteCoursecode ;}
    public Long getTermId(){return termId;}
    public String getTermCode(){return termCode;}
    public String getTermName(){return termName;}
}