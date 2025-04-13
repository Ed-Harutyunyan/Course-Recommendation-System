package edu.aua.course_recommendation.mappers;

import edu.aua.course_recommendation.dto.*;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseDto toCourseDto(CourseOfferingDto courseOfferingDto) {
        return new CourseDto(
                courseOfferingDto.courseCode(),
                courseOfferingDto.courseTitle(),
                courseOfferingDto.courseDescription(),
                courseOfferingDto.prerequisites(),
                courseOfferingDto.credits(),
                courseOfferingDto.themes()
        );
    }

    public CourseResponseDto toCourseResponseDto(Course course) {
        return new CourseResponseDto(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getDescription()
        );
    }

    public DetailedCourseResponseDto toDetailedCourseResponseDto(Course course) {
        return new DetailedCourseResponseDto(
                course.getCode(),
                course.getTitle(),
                course.getDescription(),
                course.getCredits(),
                course.getPrerequisites(),
                course.getThemes()
        );
    }



    public CourseOfferingResponseDto toCourseOfferingResponseDto(CourseOffering offering) {
        return new CourseOfferingResponseDto(
                offering.getId(),
                offering.getBaseCourse().getCode(),
                offering.getBaseCourse().getTitle(),
                String.join(", ", offering.getBaseCourse().getPrerequisites()),
                offering.getSection(),
                offering.getSession(),
                String.valueOf(offering.getBaseCourse().getCredits()),
                offering.getCampus(),
                offering.getInstructor().getName(),
                offering.getTimes(),
                offering.getTakenSeats(),
                offering.getSpacesWaiting(),
                offering.getDeliveryMethod(),
                offering.getDistLearning(),
                offering.getLocation(),
                offering.getBaseCourse().getDescription(),
                offering.getBaseCourse().getThemes(),
                offering.getYear(),
                offering.getSemester()
        );
    }
}