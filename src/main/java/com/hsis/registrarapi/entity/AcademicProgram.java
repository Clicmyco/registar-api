package com.hsis.registrarapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "academic_programs")
public class AcademicProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String programCode; // e.g. "BSC-CS"

    @Column(nullable = false)
    private String name; // e.g. "B.Sc. in Computer Science"

    @Column(nullable = false, length = 50)
    private String degreeType; // e.g. "Undergraduate", "Graduate"

    @Column(nullable = false)
    private String department; // e.g. "School of Computer Science"

    private int durationYears;

    private int totalCredits;

    @Column(length = 1000)
    private String description;

    public AcademicProgram() {}

    public AcademicProgram(String programCode, String name, String degreeType,
                           String department, int durationYears, int totalCredits, String description) {
        this.programCode = programCode;
        this.name = name;
        this.degreeType = degreeType;
        this.department = department;
        this.durationYears = durationYears;
        this.totalCredits = totalCredits;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDegreeType() { return degreeType; }
    public void setDegreeType(String degreeType) { this.degreeType = degreeType; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public int getDurationYears() { return durationYears; }
    public void setDurationYears(int durationYears) { this.durationYears = durationYears; }
    public int getTotalCredits() { return totalCredits; }
    public void setTotalCredits(int totalCredits) { this.totalCredits = totalCredits; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}