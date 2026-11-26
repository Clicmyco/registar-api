package com.hsis.registrarapi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "academic_terms")
public class AcademicTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code; // e.g., "2026-FALL"

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Fall 2026"

    @Column(name = "registration_open", nullable = false)
    private boolean registrationOpen = true;

    @Column(name = "add_drop_deadline")
    private LocalDate addDropDeadline;

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    public AcademicTerm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isRegistrationOpen() { return registrationOpen; }
    public void setRegistrationOpen(boolean registrationOpen) { this.registrationOpen = registrationOpen; }

    public LocalDate getAddDropDeadline() { return addDropDeadline; }
    public void setAddDropDeadline(LocalDate addDropDeadline) { this.addDropDeadline = addDropDeadline; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}