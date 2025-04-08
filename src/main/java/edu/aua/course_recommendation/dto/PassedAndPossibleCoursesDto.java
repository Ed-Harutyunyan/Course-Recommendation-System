package edu.aua.course_recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Builder
public record PassedAndPossibleCoursesDto (
        List<UUID> passed_ids,
        List<UUID> possible_ids
) {}
