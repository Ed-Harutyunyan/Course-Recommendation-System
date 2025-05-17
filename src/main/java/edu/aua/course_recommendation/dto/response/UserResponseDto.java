package edu.aua.course_recommendation.dto.response;

public record UserResponseDto(
        String id,
        String username,
        String role,
        String department,
        String academicStanding,
        String email,
        String profilePictureUrl,
        String createdAt,
        String updatedAt,
        boolean emailVerified
) {

}
