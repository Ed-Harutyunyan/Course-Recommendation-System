package edu.aua.course_recommendation.dto;

public record EnrollmentRequestDto(
        String courseCode,
        String grade,
        String year,
        String semester
) {}