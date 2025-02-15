package edu.aua.course_recommendation.dto;

import edu.aua.course_recommendation.model.Role;

public record UserProfileDto(String email, String username, Role role) {
}
