package edu.aua.course_recommendation.dto;

import java.util.List;

public record KeywordAndPossibleCourseDto(
    List<String> keywords,
    List<String> possibleCourseIds
) {}
