package edu.aua.course_recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationDto (
        String id,
        String courseCode,
        String courseTitle,
        String courseDescription,
        String score
) {}
