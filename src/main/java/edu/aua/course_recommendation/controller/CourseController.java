package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.response.CourseDto;
import edu.aua.course_recommendation.dto.response.CourseOfferingDto;
import edu.aua.course_recommendation.dto.response.CourseOfferingResponseDto;
import edu.aua.course_recommendation.dto.response.DetailedCourseResponseDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.util.AcademicCalendarUtil;
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
    public ResponseEntity<DetailedCourseResponseDto> getCourseByCode(@RequestParam String code) {
        return ResponseEntity.ok(courseMapper.toDetailedCourseResponseDto(courseService.getCourseByCode(code)));
    }

    @GetMapping("/all")
    public ResponseEntity<List<DetailedCourseResponseDto>> getAllCourses() {
        return ResponseEntity.ok(courseService
                .getAllCourses().stream()
                .map(courseMapper::toDetailedCourseResponseDto)
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

    @GetMapping("/offering/current")
    public ResponseEntity<List<CourseOfferingResponseDto>> getCurrentCourseOfferings() {
        String[] nextPeriod = AcademicCalendarUtil.getNextAcademicPeriod();
        List<CourseOffering> offerings = courseOfferingService.getCourseOfferingsByYearAndSemester(nextPeriod[0], nextPeriod[1]);
        List<CourseOfferingResponseDto> responseDtos = offerings.stream()
                .map(courseMapper::toCourseOfferingResponseDto)
                .toList();
        return ResponseEntity.ok(responseDtos);
    }


    @GetMapping("/offering/{year}/{semester}")
    public ResponseEntity<List<CourseOfferingResponseDto>> getAllCourseOfferingsByYearAndSemester(
            @PathVariable String year,
            @PathVariable String semester) {
        List<CourseOffering> offerings = courseOfferingService.getAllCourseOfferingsByYearAndSemester(year, semester);

        List<CourseOfferingResponseDto> responseDtos = offerings.stream()
                .map(courseMapper::toCourseOfferingResponseDto)
                .toList();

        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Retrieves course offerings for a specific academic year, semester, and course code.
     *
     * @param year     The academic year (e.g., "2023").
     * @param semester The semester (e.g., "Fall" or "Spring").
     * @param code     The course code (e.g., "CS101").
     * @return A list of course offerings matching the specified criteria.
     */
    @GetMapping("/offering/{year}/{semester}/course")
    public ResponseEntity<List<CourseOfferingResponseDto>> getCourseOfferingsByYearSemesterAndCode(
            @PathVariable String year,
            @PathVariable String semester,
            @RequestParam String code) {
        List<CourseOffering> offerings = courseOfferingService.getCourseOfferingsByYearSemesterAndCode(year, semester, code);
        List<CourseOfferingResponseDto> responseDtos = offerings.stream()
                .map(courseMapper::toCourseOfferingResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
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
