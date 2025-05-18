package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.request.InstructorProfileRequestDto;
import edu.aua.course_recommendation.dto.response.CourseOfferingResponseDto;
import edu.aua.course_recommendation.dto.response.InstructorResponseDto;
import edu.aua.course_recommendation.dto.response.InstructorWithCoursesDto;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.entity.Instructor;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.mappers.InstructorMapper;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.InstructorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;
    private final InstructorMapper instructorMapper;
    private final CourseOfferingService courseOfferingService;
    private final CourseMapper courseOfferingMapper;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
    public ResponseEntity<String> getProfessorDashboard() {
        return ResponseEntity.ok("Welcome, professor dashboard!");
    }

    @GetMapping("/all")
    public ResponseEntity<List<InstructorResponseDto>> getAllInstructors() {
        List<Instructor> instructors = instructorService.getAllInstructors();
        return ResponseEntity.ok(instructorMapper.toResponseDtoList(instructors));
    }

    @PostMapping("/add-instructors-data")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<InstructorResponseDto>> addInstructorsData(@RequestBody List<InstructorProfileRequestDto> profileDtos) {
        List<Instructor> updatedInstructors = profileDtos.stream()
                .map(instructorService::updateInstructorProfile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(instructorMapper.toResponseDtoList(updatedInstructors));
    }

    @PostMapping("/add-instructor-data")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InstructorResponseDto> addInstructor(@RequestBody InstructorProfileRequestDto profileDto) {
        Instructor updatedInstructor = instructorService.updateInstructorProfile(profileDto);
        return ResponseEntity.ok(instructorMapper.toResponseDto(updatedInstructor));
    }

    @GetMapping("/{instructorId}/offerings")
    public ResponseEntity<InstructorWithCoursesDto> getInstructorWithCoursesByYearAndSemester(
            @PathVariable UUID instructorId,
            @RequestParam String year,
            @RequestParam String semester) {

        // Get the instructor
        Instructor instructor = instructorService.getInstructorById(instructorId);

        // Get course offerings for this instructor in the specified year and semester
        List<CourseOffering> courseOfferings = courseOfferingService.getAllCourseOfferingsByYearAndSemesterAndInstructor(
                year, semester, instructorId);

        // Map course offerings to DTOs
        List<CourseOfferingResponseDto> courseOfferingDtos = courseOfferings.stream()
                .map(courseOfferingMapper::toCourseOfferingResponseDto)
                .collect(Collectors.toList());

        // Create and return the InstructorWithCoursesDto
        InstructorWithCoursesDto response = instructorMapper.toInstructorWithCoursesDto(instructor, courseOfferingDtos);

        return ResponseEntity.ok(response);
    }

}
