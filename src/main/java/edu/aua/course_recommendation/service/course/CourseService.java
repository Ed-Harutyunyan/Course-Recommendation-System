package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.exceptions.CourseAlreadyExistsException;
import edu.aua.course_recommendation.exceptions.CourseNotFoundException;
import edu.aua.course_recommendation.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {
    
    private final CourseRepository courseRepository;

    // Create a BASE Course
    // Separate from CourseOffering
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


    public Course getOrCreateCourse(CourseDto courseDto) {
        return courseRepository.findByCode(courseDto.courseCode())
                .orElseGet(() -> courseRepository.save(createCourse(courseDto)));
    }

    public void deleteCourse(String code) {
        if (!courseRepository.existsByCode(code)) {
            throw new CourseNotFoundException("Course not found with code: " + code);
        }
        courseRepository.deleteByCode(code);
    }

    public Course getCourseByCode(String code) {
        return courseRepository.findByCode(code)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with code: " + code));
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
}
