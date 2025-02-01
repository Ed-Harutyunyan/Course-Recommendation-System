package edu.aua.course_recommendation.dto;

public record RegistrationResponseDto(
        String username,
        String email,
        boolean emailVerificationRequired
) {
}
