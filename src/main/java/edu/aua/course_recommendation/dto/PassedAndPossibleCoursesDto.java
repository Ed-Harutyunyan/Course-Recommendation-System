package edu.aua.course_recommendation.dto;

import java.util.List;

public record PassedAndPossibleCoursesDto (
        List<String> passedCourseCodes, // TODO: Maybe UUID?
        List<String> possibleCourseCodes
) {}
