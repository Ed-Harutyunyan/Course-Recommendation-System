package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{studentId}/enroll/{courseId}")
    public ResponseEntity<String> enroll(@PathVariable final UUID studentId, @PathVariable final UUID courseId) {
        enrollmentService.enroll(studentId, courseId);
        return ResponseEntity.ok("Enrolled successfully");
    }
    
    @DeleteMapping("/{studentId}/drop/{courseId}")
    public ResponseEntity<String> drop(@PathVariable final UUID studentId, @PathVariable final UUID courseId) {
        enrollmentService.drop(studentId, courseId);
        return ResponseEntity.ok("Course dropped successfully");
    }
}
