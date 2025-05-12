package edu.aua.course_recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record CourseOfferingResponseDto(
        UUID id,
        @JsonProperty("course_code") String courseCode,
        @JsonProperty("course_title") String courseTitle,
        String prerequisites,
        String section,
        String session,
        String credits,
        String campus,
        String instructor,
        String times,
        @JsonProperty("taken_seats") String takenSeats,
        @JsonProperty("spaces_waiting") String spacesWaiting,
        @JsonProperty("delivery_method") String deliveryMethod,
        @JsonProperty("dist_learning") String distLearning,
        String location,
        @JsonProperty("course_description") String courseDescription,
        List<Integer> themes,
        String year,
        String semester
) {}