package com.hsis.registrarapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actorEmail; // Who performed the action

    @Column(nullable = false, length = 60)
    private String action; // e.g. "COURSE_CREATED", "ROLE_UPDATED"

    @Column(nullable = false, length = 60)
    private String targetEntity; // e.g. "Course", "User", "Enrollment"

    @Column(length = 500)
    private String details; // Human-readable summary

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(String actorEmail, String action, String targetEntity, String details) {
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetEntity = targetEntity;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; }
    public String getTargetEntity() { return targetEntity; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}