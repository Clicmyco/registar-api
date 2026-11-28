package com.hsis.registrarapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "academic_holds")
public class AcademicHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ADVISING, FINANCIAL, ACADEMIC, ADMINISTRATIVE
    @Column(nullable = false, length = 30)
    private String holdType;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, length = 120)
    private String placedByEmail;

    @Column(nullable = false)
    private LocalDateTime placedAt;

    @Column(length = 120)
    private String resolvedByEmail;

    private LocalDateTime resolvedAt;

    @Column(length = 300)
    private String resolutionNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getHoldType() { return holdType; }
    public void setHoldType(String holdType) { this.holdType = holdType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getPlacedByEmail() { return placedByEmail; }
    public void setPlacedByEmail(String placedByEmail) { this.placedByEmail = placedByEmail; }

    public LocalDateTime getPlacedAt() { return placedAt; }
    public void setPlacedAt(LocalDateTime placedAt) { this.placedAt = placedAt; }

    public String getResolvedByEmail() { return resolvedByEmail; }
    public void setResolvedByEmail(String resolvedByEmail) { this.resolvedByEmail = resolvedByEmail; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
}