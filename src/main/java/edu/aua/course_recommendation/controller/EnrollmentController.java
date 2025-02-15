package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    
    @PostMapping("/{studentId}/drop/{courseId}")
    public ResponseEntity<String> drop(@PathVariable final UUID studentId, @PathVariable final UUID courseId) {
        enrollmentService.drop(studentId, courseId);
        return ResponseEntity.ok("Unenrolled successfully");
    }
}
