package com.hsis.registrarapi.repository;

import com.hsis.registrarapi.entity.AcademicHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicHoldRepository extends JpaRepository<AcademicHold, Long> {

    List<AcademicHold> findByStudentIdOrderByActiveDescPlacedAtDesc(Long studentId);

    List<AcademicHold> findByStudentIdAndActiveTrueOrderByPlacedAtDesc(Long studentId);

    List<AcademicHold> findByActiveTrueOrderByPlacedAtDesc();

    long countByStudentIdAndActiveTrue(Long studentId);
}