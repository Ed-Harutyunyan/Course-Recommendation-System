package edu.aua.course_recommendation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorController {

    @GetMapping("/dashboard")
    public ResponseEntity<?> getProfessorDashboard() {
        return ResponseEntity.ok("Welcome, professor dashboard!");
    }
}
