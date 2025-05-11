package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.EnrollmentRequestDto;
import edu.aua.course_recommendation.dto.EnrollmentResponseDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.Enrollment;
import edu.aua.course_recommendation.mappers.EnrollmentMapper;
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
    private final EnrollmentMapper enrollmentMapper;

    @PostMapping("/{studentId}/enroll")
    public ResponseEntity<String> enrollSingle(
            @PathVariable final UUID studentId,
            @RequestBody final EnrollmentRequestDto request) {
        enrollmentService.enroll(studentId, request.courseCode(),
                request.grade(), request.year(), request.semester());
        return ResponseEntity.ok("Enrolled successfully");
    }

    @PostMapping("/{studentId}/enroll-batch")
    public ResponseEntity<String> enrollMultiple(
            @PathVariable final UUID studentId,
            @RequestBody final List<EnrollmentRequestDto> requests) {
        enrollmentService.enrollList(studentId, requests);
        return ResponseEntity.ok("Enrolled in multiple courses successfully");
    }

    @DeleteMapping("/{studentId}/drop/{courseCode}")
    public ResponseEntity<String> drop(
            @PathVariable final UUID studentId,
            @PathVariable final String courseCode) {
        enrollmentService.drop(studentId, courseCode);
        return ResponseEntity.ok("Enrollment for course " + courseCode + " dropped successfully for student id: " + studentId);
    }

    @DeleteMapping("/{studentId}/drop/all")
    public ResponseEntity<String> dropAll(@PathVariable final UUID studentId) {
        enrollmentService.dropAll(studentId);
        return ResponseEntity.ok("All enrollments dropped successfully for student id: " + studentId);
    }

    @DeleteMapping("/drop/all")
    public ResponseEntity<String> dropAll() {
        enrollmentService.dropAllEnrollments();
        return ResponseEntity.ok("All enrollments for all students dropped successfully");
    }

    @GetMapping("/{studentId}/enrollments")
    public ResponseEntity<List<EnrollmentResponseDto>> getEnrollments(@PathVariable final UUID studentId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollments(studentId);
        return ResponseEntity.ok(enrollmentMapper.toResponseDtoList(enrollments));
    }

    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<Course>> getCourses(@PathVariable final UUID studentId) {
        return ResponseEntity.ok(enrollmentService.getCourses(studentId));
    }
}