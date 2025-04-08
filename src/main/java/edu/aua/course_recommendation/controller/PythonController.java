package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.KeywordAndPossibleIdsDto;
import edu.aua.course_recommendation.dto.PassedAndPossibleCoursesDto;
import edu.aua.course_recommendation.dto.RecommendationDto;
import edu.aua.course_recommendation.service.schedule.PythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/python")
@RequiredArgsConstructor
public class PythonController {

    private final PythonService pythonService;

    @Value("${python.sent.recommendations}")
    private String dataPath;

    @PostMapping("/send/keywords")
    public ResponseEntity<String> sendKeywords(@RequestBody KeywordAndPossibleIdsDto body) {
        return pythonService.sendKeywordsRecommendations(body);
    }

//    @PostMapping("/send/passed")
//    public ResponseEntity<String> sendPassedAndPossibleCourses(@RequestBody PassedAndPossibleCoursesDto courses) {
//        return pythonService.getRecommendationsWithPassedCourses(courses);
//    }

    @PostMapping("/send/passed")
    public ResponseEntity<List<RecommendationDto>> sendPassedAndPossibleCourses(@RequestBody PassedAndPossibleCoursesDto dto) {
        List<RecommendationDto> recommendations = pythonService.getRecommendationsWithPassedCourses(
                dto.passed_ids(),
                dto.possible_ids()
        );
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/send/courses")
    public String newCourses() {
        return pythonService.sendCourses();
    }

}
