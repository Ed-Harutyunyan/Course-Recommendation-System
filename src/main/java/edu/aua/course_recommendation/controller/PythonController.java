package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.KeywordAndPossibleIdsDto;
import edu.aua.course_recommendation.dto.PassedAndPossibleCoursesDto;
import edu.aua.course_recommendation.dto.RecommendationDto;
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

    @PostMapping("/send/keywords")
    public ResponseEntity<List<RecommendationDto>> sendKeywords(@RequestBody KeywordAndPossibleIdsDto body) {
        List<RecommendationDto> recommendations = pythonService.sendKeywordsRecommendations(body);

        return ResponseEntity.ok(recommendations);
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
