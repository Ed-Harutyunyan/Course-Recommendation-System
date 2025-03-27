package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.model.NextSemesterSchedule;
import edu.aua.course_recommendation.service.NextSemesterScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final NextSemesterScheduleService nextSemesterScheduleService;

    @GetMapping
    public ResponseEntity<NextSemesterSchedule> getNextSemesterSchedule(
            @RequestParam UUID studentId,
            @RequestParam String year,
            @RequestParam String semester) {
        return ResponseEntity.ok(nextSemesterScheduleService.getNextSemester(studentId, year, semester));
    }
}
