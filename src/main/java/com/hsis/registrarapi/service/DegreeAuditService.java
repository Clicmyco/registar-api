package com.hsis.registrarapi.service;

import com.hsis.registrarapi.dto.DegreeAuditDto;
import com.hsis.registrarapi.dto.DegreeAuditItemDto;
import com.hsis.registrarapi.dto.DegreeRequirementRequestDto;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.entity.DegreeRequirement;
import com.hsis.registrarapi.entity.Enrollment;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.DegreeRequirementRepository;
import com.hsis.registrarapi.repository.EnrollmentRepository;
import com.hsis.registrarapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DegreeAuditService {

    public static final String DEFAULT_PROGRAM_CODE = "BSC-CS";
    public static final String DEFAULT_PROGRAM_NAME = "B.Sc. Computer Science";

    private final DegreeRequirementRepository requirementRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;

    public DegreeAuditService(
            DegreeRequirementRepository requirementRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService) {
        this.requirementRepository = requirementRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DegreeAuditItemDto addRequirement(DegreeRequirementRequestDto dto) {
        String normalizedProgram = dto.programCode().trim().toUpperCase();
        String normalizedCategory = dto.category().trim().toUpperCase();

        if (!normalizedCategory.equals("CORE") && !normalizedCategory.equals("ELECTIVE")) {
            throw new RuntimeException("Category must be either CORE or ELECTIVE.");
        }

        DegreeRequirement req = new DegreeRequirement();
        req.setProgramCode(normalizedProgram);
        req.setProgramName(dto.programName().trim());
        req.setCategory(normalizedCategory);

        if ("CORE".equals(normalizedCategory)) {
            if (dto.courseId() == null) {
                throw new RuntimeException("A specific course must be selected for a CORE requirement.");
            }
            if (requirementRepository.existsByProgramCodeIgnoreCaseAndCourseId(normalizedProgram, dto.courseId())) {
                throw new RuntimeException("This course is already listed as a CORE requirement for " + normalizedProgram + ".");
            }
            Course course = courseRepository.findById(dto.courseId())
                    .orElseThrow(() -> new RuntimeException("Course not found."));
            req.setCourse(course);
            req.setCreditsRequired(course.getCredits());
            req.setDescription(
                    dto.description() != null && !dto.description().isBlank()
                            ? dto.description().trim()
                            : "Required Core Course"
            );
        } else {
            int credits = dto.creditsRequired() != null ? dto.creditsRequired() : 3;
            req.setCourse(null);
            req.setCreditsRequired(credits);
            req.setDescription(
                    dto.description() != null && !dto.description().isBlank()
                            ? dto.description().trim()
                            : "Program Elective Credits"
            );
        }

        DegreeRequirement saved = requirementRepository.save(req);

        auditLogService.log(
                "DEGREE_REQUIREMENT_ADDED",
                "DegreeRequirement",
                "Added " + saved.getCategory() + " requirement ("
                        + (saved.getCourse() != null ? saved.getCourse().getCourseCode() : saved.getCreditsRequired() + " Cr")
                        + ") to " + saved.getProgramCode()
        );

        return toCatalogItemDto(saved);
    }

    @Transactional
    public void deleteRequirement(Long requirementId) {
        DegreeRequirement existing = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Degree requirement not found."));
        requirementRepository.delete(existing);

        auditLogService.log(
                "DEGREE_REQUIREMENT_REMOVED",
                "DegreeRequirement",
                "Removed requirement #" + requirementId + " from " + existing.getProgramCode()
        );
    }

    @Transactional(readOnly = true)
    public List<DegreeAuditItemDto> getProgramRequirements(String rawProgramCode) {
        String programCode = normalizeProgramCode(rawProgramCode);
        return requirementRepository.findByProgramCodeIgnoreCaseOrderByCategoryAscIdAsc(programCode)
                .stream()
                .map(this::toCatalogItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DegreeAuditDto evaluateStudentAudit(String studentEmail, String rawProgramCode) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found."));
        return buildAudit(student, normalizeProgramCode(rawProgramCode));
    }

    @Transactional(readOnly = true)
    public DegreeAuditDto evaluateStudentAuditById(Long userId, String rawProgramCode) {
        User student = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found."));
        return buildAudit(student, normalizeProgramCode(rawProgramCode));
    }

    private DegreeAuditDto buildAudit(User student, String programCode) {
        List<DegreeRequirement> requirements =
                requirementRepository.findByProgramCodeIgnoreCaseOrderByCategoryAscIdAsc(programCode);

        String programName = requirements.isEmpty()
                ? DEFAULT_PROGRAM_NAME
                : requirements.get(0).getProgramName();

        List<Enrollment> activeEnrollments = enrollmentRepository.findByUserId(student.getId()).stream()
                .filter(e -> "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .toList();

        Map<Long, Enrollment> completedByCourseId = new HashMap<>();
        Map<Long, Enrollment> inProgressByCourseId = new HashMap<>();

        for (Enrollment e : activeEnrollments) {
            Long cid = e.getCourse().getId();
            String grade = e.getGrade();
            if (grade != null && !grade.isBlank() && !"F".equalsIgnoreCase(grade)) {
                completedByCourseId.put(cid, e);
            } else if (grade == null || grade.isBlank()) {
                inProgressByCourseId.putIfAbsent(cid, e);
            }
        }

        Set<Long> consumedCourseIds = new HashSet<>();
        List<DegreeAuditItemDto> evaluatedItems = new ArrayList<>();

        // 1. Evaluate CORE course requirements first
        for (DegreeRequirement req : requirements) {
            if (!"CORE".equalsIgnoreCase(req.getCategory()) || req.getCourse() == null) {
                continue;
            }
            Course course = req.getCourse();
            Long cid = course.getId();
            consumedCourseIds.add(cid);

            if (completedByCourseId.containsKey(cid)) {
                Enrollment done = completedByCourseId.get(cid);
                evaluatedItems.add(new DegreeAuditItemDto(
                        req.getId(),
                        "CORE",
                        cid,
                        course.getCourseCode(),
                        course.getTitle(),
                        req.getDescription(),
                        req.getCreditsRequired(),
                        req.getCreditsRequired(),
                        0,
                        "COMPLETED",
                        done.getGrade()
                ));
            } else if (inProgressByCourseId.containsKey(cid)) {
                evaluatedItems.add(new DegreeAuditItemDto(
                        req.getId(),
                        "CORE",
                        cid,
                        course.getCourseCode(),
                        course.getTitle(),
                        req.getDescription(),
                        req.getCreditsRequired(),
                        0,
                        req.getCreditsRequired(),
                        "IN_PROGRESS",
                        null
                ));
            } else {
                evaluatedItems.add(new DegreeAuditItemDto(
                        req.getId(),
                        "CORE",
                        cid,
                        course.getCourseCode(),
                        course.getTitle(),
                        req.getDescription(),
                        req.getCreditsRequired(),
                        0,
                        0,
                        "REMAINING",
                        null
                ));
            }
        }

        // 2. Pool non-core completed and in-progress credits toward ELECTIVE blocks
        int availableElectiveCompleted = completedByCourseId.values().stream()
                .filter(e -> !consumedCourseIds.contains(e.getCourse().getId()))
                .mapToInt(e -> e.getCourse().getCredits())
                .sum();

        int availableElectiveInProgress = inProgressByCourseId.values().stream()
                .filter(e -> !consumedCourseIds.contains(e.getCourse().getId()))
                .mapToInt(e -> e.getCourse().getCredits())
                .sum();

        for (DegreeRequirement req : requirements) {
            if (!"ELECTIVE".equalsIgnoreCase(req.getCategory())) {
                continue;
            }
            int needed = req.getCreditsRequired();
            int earnedForBlock = Math.min(needed, availableElectiveCompleted);
            availableElectiveCompleted -= earnedForBlock;

            int stillNeeded = needed - earnedForBlock;
            int inProgForBlock = Math.min(stillNeeded, availableElectiveInProgress);
            availableElectiveInProgress -= inProgForBlock;

            String status;
            if (earnedForBlock >= needed) {
                status = "COMPLETED";
            } else if (earnedForBlock + inProgForBlock > 0) {
                status = "IN_PROGRESS";
            } else {
                status = "REMAINING";
            }

            evaluatedItems.add(new DegreeAuditItemDto(
                    req.getId(),
                    "ELECTIVE",
                    null,
                    "ELECTIVE",
                    req.getDescription() != null ? req.getDescription() : "Elective Credit Pool",
                    req.getDescription(),
                    needed,
                    earnedForBlock,
                    inProgForBlock,
                    status,
                    null
            ));
        }

        int totalRequired = evaluatedItems.stream().mapToInt(DegreeAuditItemDto::creditsRequired).sum();
        int totalCompleted = evaluatedItems.stream().mapToInt(DegreeAuditItemDto::creditsEarned).sum();
        int totalInProgress = evaluatedItems.stream().mapToInt(DegreeAuditItemDto::creditsInProgress).sum();
        int totalRemaining = Math.max(0, totalRequired - totalCompleted - totalInProgress);

        double percentage = totalRequired > 0
                ? Math.round(((double) totalCompleted / totalRequired) * 1000.0) / 10.0
                : 0.0;

        boolean eligible = totalRequired > 0 && totalCompleted >= totalRequired;

        return new DegreeAuditDto(
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                programCode,
                programName,
                totalRequired,
                totalCompleted,
                totalInProgress,
                totalRemaining,
                percentage,
                eligible,
                evaluatedItems
        );
    }

    private DegreeAuditItemDto toCatalogItemDto(DegreeRequirement req) {
        Course c = req.getCourse();
        return new DegreeAuditItemDto(
                req.getId(),
                req.getCategory(),
                c != null ? c.getId() : null,
                c != null ? c.getCourseCode() : "ELECTIVE",
                c != null ? c.getTitle() : (req.getDescription() != null ? req.getDescription() : "Elective Credits"),
                req.getDescription(),
                req.getCreditsRequired(),
                0,
                0,
                "CONFIGURED",
                null
        );
    }

    private String normalizeProgramCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PROGRAM_CODE;
        }
        return raw.trim().toUpperCase();
    }
}