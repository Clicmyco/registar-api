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

    @PreAuthorize("hasAnyRole('ADMIN','REGISTRAR')")
    @PostMapping
   
    
public ResponseEntity<Course> createCourse(@Valid @RequestBody CourseDto courseData) {
    Course savedCourse = courseService.createCourse(courseData);
    return ResponseEntity.ok(savedCourse);
}

    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'REGISTRAR', 'ADVISOR')")
    @GetMapping
    public ResponseEntity<List<CourseResponseDto>> getAllCourses(
            @RequestParam(required = false) Long termId) {
        List<CourseResponseDto> courses = courseService.getAllCourses(termId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/my-teaching")
 @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'REGISTRAR')")
 public ResponseEntity<List<CourseResponseDto>> getMyTeachingCourses(java.security.Principal principal) {
     return ResponseEntity.ok(courseService.getCoursesByInstructor(principal.getName()));
 }

 @PutMapping("/{courseId}/instructor")
 @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
 public ResponseEntity<CourseResponseDto> assignInstructor(
         @PathVariable Long courseId,
         @RequestBody(required = false) java.util.Map<String, Long> body) {
     Long instructorId = (body != null) ? body.get("instructorId") : null;
     return ResponseEntity.ok(courseService.assignInstructor(courseId, instructorId));
 }
}