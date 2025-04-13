package edu.aua.course_recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Builder
public record PassedAndPossibleCoursesDto (
        List<String> passed_ids,
        List<String> possible_ids
) {}
