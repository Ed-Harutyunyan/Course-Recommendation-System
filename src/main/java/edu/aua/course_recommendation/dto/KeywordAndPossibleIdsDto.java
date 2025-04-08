package edu.aua.course_recommendation.dto;

import java.util.List;
import java.util.UUID;

public record KeywordAndPossibleIdsDto(
    List<String> keywords,
    List<UUID> possibleCourseIds
) {}
