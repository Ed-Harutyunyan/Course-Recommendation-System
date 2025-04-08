package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.KeywordsDto;
import edu.aua.course_recommendation.dto.PassedAndPossibleCoursesDto;
import edu.aua.course_recommendation.service.schedule.PythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/python")
@RequiredArgsConstructor
public class PythonController {

    private final PythonService pythonService;

    @Value("${python.sent.recommendations}")
    private String dataPath;

    @PostMapping("/send/keywords")
    public ResponseEntity<String> sendKeywords(@RequestBody KeywordsDto keywords) {
        return pythonService.sendKeywordsRecommendations(keywords);
    }

    @PostMapping("/send/PassedAndPossibleCourses")
    public ResponseEntity<String> sendPassedAndPossibleCourses(@RequestBody PassedAndPossibleCoursesDto courses) {
        return pythonService.getRecommendationsWithPassedCourses(courses);
    }

    @PostMapping("/send/courses")
    public String newCourses() {
        return pythonService.sendCourses();
    }

}
