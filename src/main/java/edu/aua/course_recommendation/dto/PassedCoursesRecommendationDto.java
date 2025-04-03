package edu.aua.course_recommendation.dto;

import java.util.List;
import java.util.UUID;

public record PassedCoursesRecommendationDto (
//        List<UUID> passedCourseUUIDs,
//        List<UUID> recommendedCourseUUIDs
        List<Integer> passedCourseUUIDs,
        List<Integer> recommendedCourseUUIDs
){}
