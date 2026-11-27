package com.hsis.registrarapi.repository;

import com.hsis.registrarapi.entity.DegreeRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DegreeRequirementRepository extends JpaRepository<DegreeRequirement, Long> {

    List<DegreeRequirement> findByProgramCodeIgnoreCaseOrderByCategoryAscIdAsc(String programCode);

    boolean existsByProgramCodeIgnoreCaseAndCourseId(String programCode, Long courseId);
}