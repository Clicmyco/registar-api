package com.hsis.registrarapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "degree_requirements")
public class DegreeRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String programCode;

    @Column(nullable = false, length = 120)
    private String programName;

    // "CORE" (specific required course) or "ELECTIVE" (general elective credit pool)
    @Column(nullable = false, length = 20)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private int creditsRequired;

    @Column(length = 200)
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public int getCreditsRequired() { return creditsRequired; }
    public void setCreditsRequired(int creditsRequired) { this.creditsRequired = creditsRequired; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}