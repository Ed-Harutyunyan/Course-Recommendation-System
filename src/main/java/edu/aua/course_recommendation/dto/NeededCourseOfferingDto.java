package edu.aua.course_recommendation.dto;

import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.model.Requirement;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NeededCourseOfferingDto {
    private Requirement requirement;
    private CourseOffering courseOffering;
}