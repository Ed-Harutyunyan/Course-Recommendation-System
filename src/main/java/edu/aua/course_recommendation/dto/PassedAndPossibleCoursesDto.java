package edu.aua.course_recommendation.dto;

import java.util.List;

public record PassedAndPossibleCoursesDto (
        List<Integer> passed_ids,
        List<Integer> possible_ids
) {}
