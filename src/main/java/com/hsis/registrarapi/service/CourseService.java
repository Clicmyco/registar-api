package com.hsis.registrarapi.service;


import com.hsis.registrarapi.repository.EnrollmentRepository;
import com.hsis.registrarapi.dto.CourseDto;
import com.hsis.registrarapi.dto.CourseResponseDto;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;
@Service
public class CourseService {

    

    private final CourseRepository courseRepository;
    private EnrollmentRepository enrollmentRepository;
    public CourseService(CourseRepository courseRepository , EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository=enrollmentRepository;
    }

    public Course createCourse(CourseDto courseData) {
        // 1. BUSINESS RULE: Prevent duplicate course codes
        Optional<Course> existingCourse = courseRepository.findByCourseCode(courseData.getCourseCode());
        if (existingCourse.isPresent()) {
            throw new RuntimeException("A course with this code already exists!");
        }

        // 2. Transfer data from DTO to Entity
        Course newCourse = new Course();
        newCourse.setCourseCode(courseData.getCourseCode());
        newCourse.setTitle(courseData.getTitle());
        newCourse.setCredits(courseData.getCredits());
        newCourse.setMaxCapacity(courseData.getMaxCapacity());
        // 3. Save to database
        return courseRepository.save(newCourse);
    }

    // Fetch every course from the database

    public List<CourseResponseDto> getAllCourses() {
    return courseRepository.findAll().stream()
            .map(course -> new CourseResponseDto(
                    course.getId(),
                    course.getCourseCode(),
                    course.getTitle(),
                    course.getCredits(),
                    course.getMaxCapacity(),
                    enrollmentRepository.countByCourseIdAndStatus(course.getId(), "ENROLLED")
            ))
            .collect(Collectors.toList());
}
}