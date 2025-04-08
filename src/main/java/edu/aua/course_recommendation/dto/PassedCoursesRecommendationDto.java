package edu.aua.course_recommendation.dto;

import java.util.List;

public record PassedCoursesRecommendationDto (
//        List<UUID> passedCourseUUIDs,
//        List<UUID> recommendedCourseUUIDs
        List<Integer> passedCourseUUIDs,
        List<Integer> recommendedCourseUUIDs
){}
