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

    @PostMapping("/{studentId}/enroll/{courseOfferingId}")
    public ResponseEntity<String> enroll(@PathVariable final UUID studentId, @PathVariable final UUID courseOfferingId) {
        enrollmentService.enroll(studentId, courseOfferingId);
        return ResponseEntity.ok("Enrolled successfully");
    }
    
    @PostMapping("/{studentId}/drop/{courseOfferingId}")
    public ResponseEntity<String> drop(@PathVariable final UUID studentId, @PathVariable final UUID courseOfferingId) {
        enrollmentService.drop(studentId, courseOfferingId);
        return ResponseEntity.ok("Course dropped successfully");
    }
}
