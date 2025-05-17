package edu.aua.course_recommendation.dto.request;

public record InstructorProfileRequestDto(
        String name,
        String image_url,
        String position,
        String mobile,
        String email,
        String bio,
        String office_location
) {
}
