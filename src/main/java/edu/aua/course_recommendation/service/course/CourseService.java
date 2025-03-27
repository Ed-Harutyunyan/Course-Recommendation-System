package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.exceptions.CourseAlreadyExistsException;
import edu.aua.course_recommendation.exceptions.CourseNotFoundException;
import edu.aua.course_recommendation.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    
    private final CourseRepository courseRepository;

    // ============================
    // READ operations
    // ============================
    @Transactional(readOnly = true)
    public Course getCourseByCode(String code) {
        return courseRepository.findByCode(code)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with code: " + code));
    }

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByThemes(List<Integer> themes) {
        return courseRepository.findByThemes(themes);
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByTheme(Integer theme) {
        return courseRepository.findByTheme(theme);
    }

    @Transactional(readOnly = true)
    public List<Course> getLowerDivisionCourses() {
        return courseRepository.findAll().stream()
                .filter(course -> this.isLowerDivision(course.getCode()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Course> getUpperDivisionCourses() {
        return courseRepository.findAll().stream()
                .filter(course -> this.isUpperDivision(course.getCode()))
                .collect(Collectors.toList());
    }

    // ============================
    // CREATE operations
    // ============================
    @Transactional
    public Course createCourse(CourseDto courseDto) {

        if (courseRepository.existsByCode(courseDto.courseCode())) {
            throw new CourseAlreadyExistsException("Course already exists with code: " + courseDto.courseCode());
        }

        Course course = Course.builder()
                .code(courseDto.courseCode())
                .title(courseDto.courseTitle())
                .description(courseDto.courseDescription())
                .credits(Integer.valueOf(courseDto.credits())) // 0 if no credits
                .themes(courseDto.themes())
                .build();

        String prerequisites = courseDto.prerequisites();
        if (prerequisites != null && !prerequisites.isEmpty()) {
            String[] prerequisiteCodes = prerequisites.split(",");
            course.getPrerequisites().addAll(Arrays.asList(prerequisiteCodes));
        }

        return courseRepository.save(course);
    }

    @Transactional
    public Course getOrCreateCourse(CourseDto courseDto) {
        return courseRepository.findByCode(courseDto.courseCode())
                .orElseGet(() -> courseRepository.save(createCourse(courseDto)));
    }

    @Transactional
    public List<Course> createCourses(List<CourseDto> courseDtos) {
        List<Course> createdCourses = new ArrayList<>();

        for (CourseDto dto : courseDtos) {
            boolean courseExists = courseRepository.existsByCode(dto.courseCode());

            if (courseExists) {
                log.info("Skipping existing course: code={}", dto.courseCode());
                continue; // Skip if already exists
            }

            Course course = createCourse(dto);

            createdCourses.add(course);
        }

        return courseRepository.saveAll(createdCourses); // Saves all at once
    }

    // ============================
    // DELETE operations
    // ============================
    @Transactional
    public void deleteCourse(String code) {
        if (!courseRepository.existsByCode(code)) {
            throw new CourseNotFoundException("Course not found with code: " + code);
        }
        courseRepository.deleteByCode(code);
    }

    @Transactional
    public void deleteAllCourses() {
        courseRepository.deleteAll();
    }

    // All courses that code's begin with "FND110"
    @Transactional
    public Set<Course> getAllPhysedCourses() {
        return getAllCourses().stream().filter(course -> course.getCode().startsWith("FND110")).collect(Collectors.toSet());
    }

    @Transactional
    public List<Course> getAllCoursesWithThemes() {
        return getAllCourses().stream().filter(course -> !course.getThemes().isEmpty()).collect(Collectors.toList());
    }

    /**
     * If the code starts with '1', then its lower-division.
     * Otherwise upper-division.
     */
    public boolean isLowerDivision(String courseCode) {
        String code = courseCode.replaceAll("[^0-9]", "");
        if (code.isEmpty()) return false;

        try {
            int number = Integer.parseInt(code);
            return number < 200; // Lower division courses are numbered < 200
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public boolean isUpperDivision(String courseCode) {
        return !isLowerDivision(courseCode);
    }

}
