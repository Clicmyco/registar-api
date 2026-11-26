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
}