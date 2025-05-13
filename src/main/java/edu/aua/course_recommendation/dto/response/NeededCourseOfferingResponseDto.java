package edu.aua.course_recommendation.dto.response;

import edu.aua.course_recommendation.model.Requirement;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NeededCourseOfferingResponseDto {
    private Requirement requirement;
    private CourseOfferingResponseDto courseOffering;
}