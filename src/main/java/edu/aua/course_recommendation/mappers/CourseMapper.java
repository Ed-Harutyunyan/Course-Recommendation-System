package edu.aua.course_recommendation.mappers;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.dto.CourseOfferingDto;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseDto toCourseDto(CourseOfferingDto courseOfferingDto) {
        return new CourseDto(
                courseOfferingDto.courseCode(),
                courseOfferingDto.courseTitle(),
                courseOfferingDto.courseDescription(),
                courseOfferingDto.prerequisites(),
                String.valueOf(courseOfferingDto.credits()),
                courseOfferingDto.themes()
        );
    }
}