package edu.aua.course_recommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.dto.KeywordsDto;
import edu.aua.course_recommendation.service.PythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/python")
@RequiredArgsConstructor
public class PythonController {

    private final PythonService pythonService;

    @Value("${python.sent.recommendations}")
    private String dataPath;

    @PostMapping("/sendKeywords")
    public ResponseEntity<String> sendKeywords(@RequestBody KeywordsDto keywords) {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(String.format(dataPath, "received_payload.json"));

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            objectMapper.writeValue(file, pythonService.sendKeywordsRecommendations(keywords));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error saving JSON to file");
        }

        return ResponseEntity.ok("JSON received and saved successfully");
    }

    @PostMapping("/newCourses")
    public String newCourses(@RequestBody List<CourseDto> data) {
        return pythonService.sendCourses(data);
    }

}
