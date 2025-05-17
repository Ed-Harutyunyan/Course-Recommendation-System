package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.request.InstructorProfileRequestDto;
import edu.aua.course_recommendation.dto.response.InstructorResponseDto;
import edu.aua.course_recommendation.entity.Instructor;
import edu.aua.course_recommendation.mappers.InstructorMapper;
import edu.aua.course_recommendation.service.course.InstructorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;
    private final InstructorMapper instructorMapper;

    @GetMapping("/dashboard")
    public ResponseEntity<String> getProfessorDashboard() {
        return ResponseEntity.ok("Welcome, professor dashboard!");
    }

    @GetMapping("/all")
    public ResponseEntity<List<InstructorResponseDto>> getAllInstructors() {
        List<Instructor> instructors = instructorService.getAllInstructors();
        return ResponseEntity.ok(instructorMapper.toResponseDtoList(instructors));
    }

    @PostMapping("/add-instructors-data")
    public ResponseEntity<List<InstructorResponseDto>> addInstructorsData(@RequestBody List<InstructorProfileRequestDto> profileDtos) {
        List<Instructor> updatedInstructors = profileDtos.stream()
                .map(instructorService::updateInstructorProfile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(instructorMapper.toResponseDtoList(updatedInstructors));
    }

}
