package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.Enrollment;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{studentId}/enroll/{courseCode}")
    public ResponseEntity<String> enroll(
            @PathVariable final UUID studentId,
            @PathVariable final String courseCode) {
        enrollmentService.enroll(studentId, courseCode);
        return ResponseEntity.ok("Enrolled successfully");
    }

    @PostMapping("/{studentId}/enroll")
    public ResponseEntity<String> enroll(@PathVariable final UUID studentId, @RequestBody final List<String> courseCodes) {
        enrollmentService.enrollList(studentId, courseCodes);
        return ResponseEntity.ok("Enrolled successfully");
    }

    @PostMapping("/{studentId}/enroll/all")
    public ResponseEntity<String> enrollAll(@PathVariable final UUID studentId) {
        enrollmentService.enrollAll(studentId);
        return ResponseEntity.ok("Enrolled in all courses successfully");
    }
    
    @DeleteMapping("/{studentId}/drop/{courseCode}")
    public ResponseEntity<String> drop(@PathVariable final UUID studentId, @PathVariable final String courseCode) {
        enrollmentService.drop(studentId, courseCode);
        return ResponseEntity.ok("Enrollment dropped successfully");
    }

    @DeleteMapping("/{studentId}/drop/all")
    public ResponseEntity<String> dropAll(@PathVariable final UUID studentId) {
        enrollmentService.dropAll(studentId);
        return ResponseEntity.ok("All enrollments dropped successfully");
    }

    @DeleteMapping("/drop/all")
    public ResponseEntity<String> dropAll() {
        enrollmentService.dropAllEnrollments();
        return ResponseEntity.ok("All enrollments for all students dropped successfully");
    }

    @GetMapping("/{studentId}/enrollments")
    public ResponseEntity<List<Enrollment>> getEnrollments(@PathVariable final UUID studentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollments(studentId));
    }

    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<Course>> getCourses(@PathVariable final UUID studentId) {
        return ResponseEntity.ok(enrollmentService.getCourses(studentId));
    }
}
