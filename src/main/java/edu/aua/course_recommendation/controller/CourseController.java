package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.dto.CourseOfferingDto;
import edu.aua.course_recommendation.dto.CourseOfferingResponseDto;
import edu.aua.course_recommendation.dto.CourseResponseDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
public class CourseController {

    private final CourseService courseService;
    private final CourseOfferingService courseOfferingService;
    private final CourseMapper courseMapper;

    /*
     * BASE COURSE RELATED ENDPOINTS
     */

    @GetMapping
    public ResponseEntity<Course> getCourseByCode(@RequestParam String code) {
        return ResponseEntity.ok(courseService.getCourseByCode(code));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CourseResponseDto>> getAllCourses() {
        return ResponseEntity.ok(courseService
                .getAllCourses().stream()
                .map(courseMapper::toCourseResponseDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/all/themes")
    public ResponseEntity<List<Course>> getCoursesByThemes(@RequestParam List<Integer> themes) {
        return ResponseEntity.ok(courseService.getCoursesByThemes(themes));
    }

    @GetMapping("/all/theme")
    public ResponseEntity<List<Course>> getCoursesByTheme(@RequestParam Integer theme) {
        return ResponseEntity.ok(courseService.getCoursesByTheme(theme));
    }

    @GetMapping("/all/themes/any")
    public ResponseEntity<List<Course>> getAllCoursesWithThemes() {
        return ResponseEntity.ok(courseService.getAllCoursesWithThemes());
    }

    @PostMapping("/create")
    public ResponseEntity<String> createCourse(@RequestBody CourseDto courseDto) {
        Course createdCourse = courseService.createCourse(courseDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Course created successfully with code: " + createdCourse.getCode());
    }

    @PostMapping("/create/all")
    public ResponseEntity<String> createCourses(@RequestBody List<CourseDto> courseDtos) {
        List<Course> createdCourses = courseService.createCourses(courseDtos);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Base courses created successfully: " + createdCourses.size());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteCourse(@RequestParam String code) {
        courseService.deleteCourse(code);
        return ResponseEntity.ok("Course deleted successfully");
    }

    @DeleteMapping("/delete/all")
    public ResponseEntity<String> deleteAllCourses() {
        courseService.deleteAllCourses();
        return ResponseEntity.ok("All Base Course deleted successfully");
    }

    /*
     * OFFERINGS RELATED ENDPOINTS
     */

    @GetMapping("/offering/all")
    public ResponseEntity<List<CourseOfferingResponseDto>> getAllCourseOfferings() {
        List<CourseOffering> offerings = courseOfferingService.getAllCourseOfferings();
        List<CourseOfferingResponseDto> responseDtos = offerings.stream()
                .map(courseMapper::toCourseOfferingResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/offering")
    public ResponseEntity<CourseOfferingResponseDto> getCourseOfferingById(@RequestParam UUID id) {
        CourseOffering offering = courseOfferingService.getCourseOfferingById(id);
        return ResponseEntity.ok(courseMapper.toCourseOfferingResponseDto(offering));
    }

    @PostMapping("/offering/create")
    public ResponseEntity<String> createCourseOffering(@RequestBody CourseOfferingDto offeringDto) {
        CourseOffering offering = courseOfferingService.createCourseOffering(offeringDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Course offering created for Course with code: " + offering.getBaseCourse().getCode());
    }

    @PostMapping("/offering/create/all")
    public ResponseEntity<String> createCourseOfferings(@RequestBody List<CourseOfferingDto> courseOfferingDtos) {
        List<CourseOffering> createdOfferings = courseOfferingService.createCourseOfferings(courseOfferingDtos);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Course offerings created successfully: " + createdOfferings.size());
    }

    @DeleteMapping("/offering/delete/all")
    public ResponseEntity<String> deleteAllCourseOfferings() {
        courseOfferingService.deleteAllCourseOfferings();
        return ResponseEntity.ok("All course offerings deleted successfully");
    }

    @DeleteMapping("/offering/delete")
    public ResponseEntity<String> deleteCourseOffering(@RequestParam UUID id) {
        courseOfferingService.deleteCourseOfferingById(id);
        return ResponseEntity.ok("Course offering deleted successfully");
    }

}
