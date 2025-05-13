package edu.aua.course_recommendation.dto.request;

import java.util.List;

public record PassedCoursesRecommendationDto (
//        List<UUID> passedCourseUUIDs,
//        List<UUID> recommendedCourseUUIDs
        List<Integer> passedCourseUUIDs,
        List<Integer> recommendedCourseUUIDs
){}
