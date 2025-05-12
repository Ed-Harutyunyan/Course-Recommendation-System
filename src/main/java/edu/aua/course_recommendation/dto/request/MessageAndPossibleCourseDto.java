package edu.aua.course_recommendation.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record MessageAndPossibleCourseDto(
    String message,
    List<String> possibleCourseCodes
) {}
