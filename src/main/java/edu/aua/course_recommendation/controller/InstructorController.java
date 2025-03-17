package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.entity.Instructor;
import edu.aua.course_recommendation.service.course.InstructorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;

    @GetMapping("/dashboard")
    public ResponseEntity<String> getProfessorDashboard() {
        return ResponseEntity.ok("Welcome, professor dashboard!");
    }

    @GetMapping("/all")
    public ResponseEntity<List<Instructor>> getAllInstructors() {
        return ResponseEntity.ok(instructorService.getAllInstructors());
    }

}
