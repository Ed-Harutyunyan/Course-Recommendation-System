package edu.aua.course_recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationDto (
        String id,
        String courseCode,
        String courseTitle,
        String courseDescription,
        String score
) {}
