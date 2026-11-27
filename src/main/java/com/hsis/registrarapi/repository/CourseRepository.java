package com.hsis.registrarapi.repository;

import com.hsis.registrarapi.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    java.util.List<com.hsis.registrarapi.entity.Course> findByTermId(Long termId);
    java.util.List<com.hsis.registrarapi.entity.Course> findByInstructorEmail(String email);
    // We will need this to prevent creating duplicate courses
    Optional<Course> findByCourseCode(String courseCode);
}