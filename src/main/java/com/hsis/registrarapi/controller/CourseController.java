package com.hsis.registrarapi.controller;

import com.hsis.registrarapi.dto.CourseDto;
import com.hsis.registrarapi.dto.CourseResponseDto;
import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.service.CourseService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PreAuthorize("hasRole('ADMIN','REGISTRAR')")
    @PostMapping
   
    
public ResponseEntity<Course> createCourse(@Valid @RequestBody CourseDto courseData) {
    Course savedCourse = courseService.createCourse(courseData);
    return ResponseEntity.ok(savedCourse);
}

    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN','REGISTRAR','ADVISOR')")
    @GetMapping
    public ResponseEntity<List<CourseResponseDto>> getAllCourses() {
        List<CourseResponseDto> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }
}