package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class CourseService {
    
    private final CourseRepository courseRepository;


    // Create a BASE Course
    // Separate from CourseOffering
    public Course createCourse(CourseDto courseDto) {
        if (courseRepository.existsByCode(courseDto.courseCode())) {
            throw new IllegalArgumentException("Course already exists");
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
}
