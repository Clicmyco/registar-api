package com.hsis.registrarapi.repository;

import com.hsis.registrarapi.entity.AcademicProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicProgramRepository extends JpaRepository<AcademicProgram, Long> {
    Optional<AcademicProgram> findByProgramCode(String programCode);
}