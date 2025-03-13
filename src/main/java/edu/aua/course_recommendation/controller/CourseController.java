package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.dto.CourseOfferingDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.service.CourseOfferingService;
import edu.aua.course_recommendation.service.CourseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
public class CourseController {

    private final ObjectMapper objectMapper;
    private final CourseService courseService;
    private final CourseOfferingService courseOfferingService;

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getCourseDetails() {
        Map<String, Object> response = new HashMap<>();
        response.put("courseName", "Java Basics");
        response.put("courseId", "101");
        response.put("instructor", "John Doe");
        response.put("description", "Introduction to Java programming");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recommendations")
    public ResponseEntity<String> postRecommendedCourses(@RequestBody List<Map<String, Object>> payload) {
        File file = new File("src/main/resources/data/received_payload.json");

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            objectMapper.writeValue(file, payload);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error saving JSON to file");
        }

        return ResponseEntity.ok("JSON received and saved successfully");
    }

    @GetMapping
    public ResponseEntity<Course> getCourseByCode(@RequestParam String code) {
        return ResponseEntity.ok(courseService.getCourseByCode(code));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    // This endpoint takes a Json payload and creates a course
    // This is only a "base course" used for the algorithm
    // Other data in the JSON will be stored along with a reference to this course
    // in course offering
    @PostMapping("/create")
    @Transactional
    public ResponseEntity<String> createCourse(@RequestBody CourseDto courseDto) {
        Course createdCourse = courseService.createCourse(courseDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Course created successfully with code: " + createdCourse.getCode());
    }

    @PostMapping("/create-offering")
    @Transactional
    public ResponseEntity<String> createCourseOffering(@RequestBody CourseOfferingDto offeringDto) {
        CourseOffering offering = courseOfferingService.createCourseOffering(offeringDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Course offering created for Course with code: " + offering.getBaseCourse().getCode());
    }

    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<String> deleteCourse(@RequestParam String code) {
        courseService.deleteCourse(code);
        return ResponseEntity.ok("Course deleted successfully");
    }

}
