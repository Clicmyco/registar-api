
package com.hsis.registrarapi.repository;

import com.hsis.registrarapi.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    Optional<AcademicTerm> findByActiveTrue();
    Optional<AcademicTerm> findByCodeIgnoreCase(String code);
    List<AcademicTerm> findAllByOrderByIdDesc();
}