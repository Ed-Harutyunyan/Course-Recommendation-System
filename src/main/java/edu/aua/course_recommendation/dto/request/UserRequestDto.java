package edu.aua.course_recommendation.dto.request;

public record UserRequestDto(
        String username,
        String password,
        String role,
        String department,
        String academicStanding,
        String email,
        String profilePictureUrl
) {
}
