package com.hsis.registrarapi.repository;

import com.hsis.registrarapi.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    // Find all courses a specific user is enrolled in
    List<Enrollment> findByUserId(Long userId);
    
    // Check if a student is already enrolled in a specific course to prevent double-booking
    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);
    // Find all enrollments belonging to a specific course
    List<Enrollment> findByCourseId(Long courseId);
    // Count only active ENROLLED students (excludes DROPPED records)
    long countByCourseIdAndStatus(Long courseId, String status);
    List<Enrollment> findByStatusOrderByEnrollmentDateAsc(String status);

    java.util.List<com.hsis.registrarapi.entity.Enrollment> findByCourseIdAndStatusOrderByEnrollmentDateAsc(
            Long courseId,
            String status
    );
}
