package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.AcademicHoldDto;
import com.hsis.registrarapi.dto.PlaceHoldRequestDto;
import com.hsis.registrarapi.dto.StudentLookupDto;
import com.hsis.registrarapi.entity.AcademicHold;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.AcademicHoldRepository;
import com.hsis.registrarapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class AcademicHoldService {

    private static final Set<String> VALID_HOLD_TYPES = Set.of(
            "ADVISING", "FINANCIAL", "ACADEMIC", "ADMINISTRATIVE"
    );

    private final AcademicHoldRepository holdRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AcademicHoldService(
            AcademicHoldRepository holdRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.holdRepository = holdRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AcademicHoldDto placeHold(PlaceHoldRequestDto dto, String staffEmail) {
        User student = userRepository.findById(dto.studentId())
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + dto.studentId()));

        String normalizedType = dto.holdType().trim().toUpperCase();
        if (!VALID_HOLD_TYPES.contains(normalizedType)) {
            throw new RuntimeException(
                    "Invalid hold type. Allowed: ADVISING, FINANCIAL, ACADEMIC, ADMINISTRATIVE."
            );
        }

        AcademicHold hold = new AcademicHold();
        hold.setStudent(student);
        hold.setHoldType(normalizedType);
        hold.setReason(dto.reason().trim());
        hold.setActive(true);
        hold.setPlacedByEmail(staffEmail);
        hold.setPlacedAt(LocalDateTime.now());

        AcademicHold saved = holdRepository.save(hold);

        auditLogService.log(
                "ACADEMIC_HOLD_PLACED",
                "AcademicHold",
                "Placed " + normalizedType + " hold on " + student.getEmail() + " by " + staffEmail
        );

        return toDto(saved);
    }

    @Transactional
    public AcademicHoldDto liftHold(Long holdId, String resolutionNote, String staffEmail) {
        AcademicHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new RuntimeException("Academic hold not found."));

        if (!hold.isActive()) {
            throw new RuntimeException("This hold has already been cleared.");
        }

        hold.setActive(false);
        hold.setResolvedByEmail(staffEmail);
        hold.setResolvedAt(LocalDateTime.now());
        hold.setResolutionNote(
                resolutionNote != null && !resolutionNote.isBlank()
                        ? resolutionNote.trim()
                        : "Cleared by staff"
        );

        AcademicHold saved = holdRepository.save(hold);

        auditLogService.log(
                "ACADEMIC_HOLD_LIFTED",
                "AcademicHold",
                "Lifted " + hold.getHoldType() + " hold #" + holdId + " for " + hold.getStudent().getEmail()
        );

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AcademicHoldDto> getMyActiveHolds(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found."));
        return holdRepository.findByStudentIdAndActiveTrueOrderByPlacedAtDesc(student.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicHoldDto> getStudentHolds(Long studentId) {
        return holdRepository.findByStudentIdOrderByActiveDescPlacedAtDesc(studentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicHoldDto> getAllActiveHolds() {
        return holdRepository.findByActiveTrueOrderByPlacedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentLookupDto> searchStudents(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase();

        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> "ROLE_STUDENT".equalsIgnoreCase(r.getName())))
                .filter(u -> query.isEmpty()
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(query))
                        || u.getEmail().toLowerCase().contains(query))
                .sorted(Comparator.comparing(
                        u -> u.getFullName() != null ? u.getFullName() : u.getEmail(),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(u -> new StudentLookupDto(
                        u.getId(),
                        u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail(),
                        u.getEmail(),
                        holdRepository.countByStudentIdAndActiveTrue(u.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public void assertNoActiveHolds(Long studentId) {
        List<AcademicHold> activeHolds =
                holdRepository.findByStudentIdAndActiveTrueOrderByPlacedAtDesc(studentId);

        if (!activeHolds.isEmpty()) {
            AcademicHold primary = activeHolds.get(0);
            throw new RuntimeException(
                    "Registration blocked by active " + primary.getHoldType() + " hold: "
                            + primary.getReason()
                            + (activeHolds.size() > 1 ? " (+" + (activeHolds.size() - 1) + " more)" : "")
            );
        }
    }

    private AcademicHoldDto toDto(AcademicHold hold) {
        User student = hold.getStudent();
        return new AcademicHoldDto(
                hold.getId(),
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                hold.getHoldType(),
                hold.getReason(),
                hold.isActive(),
                hold.getPlacedByEmail(),
                hold.getPlacedAt(),
                hold.getResolvedByEmail(),
                hold.getResolvedAt(),
                hold.getResolutionNote()
        );
    }
}