package edu.aua.course_recommendation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
}
