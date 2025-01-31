package edu.aua.course_recommendation.dto;

public record AuthenticationRequestDto(
        String username,
        String password
) {
}
