package edu.aua.course_recommendation.dto;

import java.util.UUID;

public record CourseResponseDto(
        UUID id,
        String courseCode,
        String courseTitle,
        String courseDescription
) {
}
