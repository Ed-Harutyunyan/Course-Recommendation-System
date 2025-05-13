package edu.aua.course_recommendation.dto.request;

public record EnrollmentRequestDto(
        String courseCode,
        String grade,
        String year,
        String semester
) {}