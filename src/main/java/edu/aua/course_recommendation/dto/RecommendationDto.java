package edu.aua.course_recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationDto (
        @JsonProperty("id")String id,
        @JsonProperty("title")String title,
        @JsonProperty("description")String description
) {}
