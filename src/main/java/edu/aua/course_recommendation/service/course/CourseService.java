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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    
    private final CourseRepository courseRepository;

    // Create a BASE Course
    // Separate from CourseOffering
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
                .clusters(courseDto.clusters())
                .build();

        String prerequisites = courseDto.prerequisites();
        System.out.println("prerequisites: " + prerequisites);
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
    public void deleteCourse(String code) {
        if (!courseRepository.existsByCode(code)) {
            throw new CourseNotFoundException("Course not found with code: " + code);
        }
        courseRepository.deleteByCode(code);
    }

    @Transactional(readOnly = true)
    public Course getCourseByCode(String code) {
        return courseRepository.findByCode(code)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with code: " + code));
    }

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
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

    @Transactional
    public void deleteAllCourses() {
        courseRepository.deleteAll();
    }
}
