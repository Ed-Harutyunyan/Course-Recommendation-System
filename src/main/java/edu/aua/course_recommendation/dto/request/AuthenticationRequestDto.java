package edu.aua.course_recommendation.dto.request;

public record AuthenticationRequestDto(
        String username,
        String password
) {
}
