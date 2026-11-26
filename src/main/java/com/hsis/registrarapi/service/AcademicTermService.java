package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.AcademicTermRequest;
import com.hsis.registrarapi.dto.TermStatusUpdateRequest;
import com.hsis.registrarapi.entity.AcademicTerm;
import com.hsis.registrarapi.repository.AcademicTermRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AcademicTermService {

    private final AcademicTermRepository termRepository;
    private final AuditLogService auditLogService;

    public AcademicTermService(AcademicTermRepository termRepository, AuditLogService auditLogService) {
        this.termRepository = termRepository;
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    @Transactional
    public void seedDefaultTerm() {
        if (termRepository.count() == 0) {
            AcademicTerm defaultTerm = new AcademicTerm();
            defaultTerm.setCode("2026-FALL");
            defaultTerm.setName("Fall Semester 2026");
            defaultTerm.setRegistrationOpen(true);
            defaultTerm.setAddDropDeadline(LocalDate.now().plusDays(14));
            defaultTerm.setActive(true);
            termRepository.save(defaultTerm);
        }
    }

    @Transactional(readOnly = true)
    public AcademicTerm getActiveTerm() {
        return termRepository.findByActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active academic term configured."));
    }

    @Transactional(readOnly = true)
    public List<AcademicTerm> getAllTerms() {
        return termRepository.findAllByOrderByIdDesc();
    }

    @Transactional
    public AcademicTerm createTerm(AcademicTermRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        if (termRepository.findByCodeIgnoreCase(normalizedCode).isPresent()) {
            throw new RuntimeException("An academic term with code " + normalizedCode + " already exists.");
        }

        boolean makeActive = Boolean.TRUE.equals(request.setActive());
        if (makeActive) {
            deactivateAllTerms();
        }

        AcademicTerm term = new AcademicTerm();
        term.setCode(normalizedCode);
        term.setName(request.name().trim());
        term.setRegistrationOpen(request.registrationOpen() == null || request.registrationOpen());
        term.setAddDropDeadline(request.addDropDeadline());
        term.setActive(makeActive);

        AcademicTerm saved = termRepository.save(term);

        auditLogService.log(
                "TERM_CREATED",
                "AcademicTerm",
                "Created term " + saved.getCode() + " (" + saved.getName() + ")"
        );
        return saved;
    }

    @Transactional
    public AcademicTerm updateTermStatus(Long termId, TermStatusUpdateRequest request) {
        AcademicTerm term = termRepository.findById(termId)
                .orElseThrow(() -> new RuntimeException("Academic term not found."));

        if (Boolean.TRUE.equals(request.active()) && !term.isActive()) {
            deactivateAllTerms();
            term.setActive(true);
        } else if (Boolean.FALSE.equals(request.active())) {
            term.setActive(false);
        }

        if (request.registrationOpen() != null) {
            term.setRegistrationOpen(request.registrationOpen());
        }

        if (request.addDropDeadline() != null) {
            term.setAddDropDeadline(request.addDropDeadline());
        }

        AcademicTerm saved = termRepository.save(term);

        auditLogService.log(
                "TERM_UPDATED",
                "AcademicTerm",
                "Updated " + saved.getCode() + " [Active=" + saved.isActive()
                        + ", RegistrationOpen=" + saved.isRegistrationOpen() + "]"
        );
        return saved;
    }

    private void deactivateAllTerms() {
        termRepository.findAll().forEach(t -> {
            if (t.isActive()) {
                t.setActive(false);
                termRepository.save(t);
            }
        });
    }
}