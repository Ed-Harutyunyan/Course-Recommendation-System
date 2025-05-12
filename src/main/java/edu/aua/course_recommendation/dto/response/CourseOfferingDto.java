package edu.aua.course_recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CourseOfferingDto(
        @JsonProperty("course_title") String courseTitle,
        @JsonProperty("course_code") String courseCode,
        @JsonProperty("prerequisites") String prerequisites,
        @JsonProperty("section") String section,
        @JsonProperty("session") String session,
        @JsonProperty("credits") String credits,
        @JsonProperty("campus") String campus,
        @JsonProperty("instructor") String instructor,
        @JsonProperty("times") String times,
        @JsonProperty("taken_seats") String takenSeats,
        @JsonProperty("spaces_waiting") String spacesWaiting,
        @JsonProperty("delivery_method") String deliveryMethod,
        @JsonProperty("dist_learning") String distLearning,
        @JsonProperty("location") String location,
        @JsonProperty("year") String year,
        @JsonProperty("semester") String semester,
        @JsonProperty("course_description") String courseDescription,
        @JsonProperty("themes") List<Integer> themes
) {}

