package edu.aua.course_recommendation.dto;

public record RegistrationRequestDto (
        String username,
        String email,
        String password
) {
}
