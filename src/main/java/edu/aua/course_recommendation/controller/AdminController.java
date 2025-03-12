package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.service.CourseService;
import edu.aua.course_recommendation.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JwtService jwtService;
    private final CourseService courseService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-token")
    public ResponseEntity<String> getServiceToken(@RequestParam String serviceName) {
        String token = jwtService.generateToken(serviceName, String.valueOf(Role.ROLE_SERVICE));
        return ResponseEntity.ok(token);
    }

    @PostMapping("/courses/create")
    public ResponseEntity<Course> createCourse(@RequestBody CourseDto courseDto) {
        Course newCourse = courseService.createCourse(courseDto);
        return new ResponseEntity<>(newCourse, HttpStatus.CREATED);
    }

}
