package edu.aua.course_recommendation.dto.response;

import java.util.UUID;

public record InstructorResponseDto(
        UUID id,
        String name,
        String imageUrl,
        String position,
        String mobile,
        String email,
        String bio,
        String officeLocation
) {
}