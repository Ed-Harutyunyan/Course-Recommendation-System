package edu.aua.course_recommendation.dto.response;

public record RegistrationResponseDto(
        String username,
        String email,
        boolean emailVerificationRequired
) {
}
