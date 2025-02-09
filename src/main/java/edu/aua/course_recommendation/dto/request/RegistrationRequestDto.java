package edu.aua.course_recommendation.dto.request;

public record RegistrationRequestDto (
        String username,
        String email,
        String password
) {
}
