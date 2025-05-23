package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.request.MessageAndPossibleCourseDto;
import edu.aua.course_recommendation.dto.request.PassedAndPossibleCoursesDto;
import edu.aua.course_recommendation.dto.response.RecommendationDto;
import edu.aua.course_recommendation.service.schedule.PythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/python")
@RequiredArgsConstructor
public class PythonController {

    private final PythonService pythonService;

    @PostMapping("/send/message")
    @PreAuthorize("hasRole('ROLE_SERVICE')")
    public ResponseEntity<List<RecommendationDto>> sendKeywords(@RequestBody MessageAndPossibleCourseDto body) {
        List<RecommendationDto> recommendations = pythonService.sendMessageRecommendations(body);

        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/send/passed")
    @PreAuthorize("hasRole('ROLE_SERVICE')")
    public ResponseEntity<List<RecommendationDto>> sendPassedAndPossibleCourses(@RequestBody PassedAndPossibleCoursesDto dto) {
        List<RecommendationDto> recommendations = pythonService.getRecommendationsWithPassedCourses(
                dto.passed_course_codes(),
                dto.possible_course_codes()
        );
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/send/courses")
    @PreAuthorize("hasRole('ROLE_SERVICE')")
    public String newCourses() {
        return pythonService.sendCourses();
    }

    @DeleteMapping("/delete/courses")
    @PreAuthorize("hasRole('ROLE_SERVICE')")
    public ResponseEntity<String> deletePointsQdrant() {
        return pythonService.deletePoints();
    }
}
