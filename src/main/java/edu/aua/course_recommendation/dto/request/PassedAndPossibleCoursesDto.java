package edu.aua.course_recommendation.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record PassedAndPossibleCoursesDto (
        List<String> passed_course_codes,
        List<String> possible_course_codes
) {}
