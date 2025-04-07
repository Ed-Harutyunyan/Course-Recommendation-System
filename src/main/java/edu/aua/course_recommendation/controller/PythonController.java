package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.dto.KeywordsDto;
import edu.aua.course_recommendation.dto.PassedAndPossibleCoursesDto;
import edu.aua.course_recommendation.service.schedule.PythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/python")
@RequiredArgsConstructor
public class PythonController {

    private final PythonService pythonService;

    @PostMapping("/sendKeywords")
    public ResponseEntity<String> sendKeywords(@RequestBody KeywordsDto keywords) {
        return pythonService.sendKeywordsRecommendations(keywords);
    }

    @PostMapping("/sendPassedAndPossibleCourses")
    public ResponseEntity<String> sendPassedAndPossibleCourses(@RequestBody PassedAndPossibleCoursesDto courses) {
        return pythonService.getRecommendationsWithPassedCourses(courses);
    }

    @PostMapping("/newCourses")
    public String newCourses(@RequestBody List<CourseDto> data) {
        return pythonService.sendCourses(data);
    }

}
