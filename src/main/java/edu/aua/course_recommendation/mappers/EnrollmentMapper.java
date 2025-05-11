package edu.aua.course_recommendation.mappers;

import edu.aua.course_recommendation.dto.EnrollmentResponseDto;
import edu.aua.course_recommendation.entity.Enrollment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EnrollmentMapper {

    public EnrollmentResponseDto toResponseDto(Enrollment enrollment) {
        var course = enrollment.getCourse();
        var courseInfo = new EnrollmentResponseDto.CourseInfo(
                course.getCode(),
                course.getTitle(),
                course.getCredits()
        );

        return new EnrollmentResponseDto(
                courseInfo,
                enrollment.getGrade(),
                enrollment.getYear(),
                enrollment.getSemester()
        );
    }

    public List<EnrollmentResponseDto> toResponseDtoList(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }
}