package com.hsis.registrarapi.entity;

import jakarta.persistence.*;



@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String courseCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int credits;

    @Column(name = "max_capacity", columnDefinition = "integer default 30")
    private Integer maxCapacity = 30;
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_id")
    private Course prerequisite;

   @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private AcademicTerm term;

    @Column(name = "meeting_days", length = 30)
    private String meetingDays; // Normalized comma-separated codes: e.g., "MON,WED,FRI"

    @Column(name = "start_time")
    private java.time.LocalTime startTime;

    @Column(name = "end_time")
    private java.time.LocalTime endTime;

    @Column(length = 50)
    private String room;

     @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private User instructor;

    public Course() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getMaxCapacity() {
        return maxCapacity != null ? maxCapacity : 30;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = (maxCapacity != null && maxCapacity > 0) ? maxCapacity : 30;
    }


    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
    
    public Course getPrerequisite() { return prerequisite; }
    public void setPrerequisite(Course prerequisite) { this.prerequisite = prerequisite; }

    public AcademicTerm getTerm() { return term; }
    public void setTerm(AcademicTerm term) { this.term = term; }

    public String getMeetingDays() { return meetingDays; }
    public void setMeetingDays(String meetingDays) { this.meetingDays = meetingDays; }

    public java.time.LocalTime getStartTime() { return startTime; }
    public void setStartTime(java.time.LocalTime startTime) { this.startTime = startTime; }

    public java.time.LocalTime getEndTime() { return endTime; }
    public void setEndTime(java.time.LocalTime endTime) { this.endTime = endTime; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public User getInstructor() { return instructor; }
    public void setInstructor(User instructor) { this.instructor = instructor; }
}