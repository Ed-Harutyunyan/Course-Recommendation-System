package edu.aua.course_recommendation.dto;

import lombok.Data;

@Data
public class EnrollmentDto {
    private CourseResponseDto course;
    private String grade;
    private String year;
    private String semester;
}