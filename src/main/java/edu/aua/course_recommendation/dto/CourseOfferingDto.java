package edu.aua.course_recommendation.dto;

import java.util.List;

public record CourseOfferingDto(
        String courseTitle,
        String courseCode,
        String prerequisites,
        String section,
        String session,
        Integer credits,
        String campus,
        String instructor,
        String times,
        String takenSeats,
        String spacesWaiting,
        String deliveryMethod,
        String distLearning,
        String location,
        String year,
        String semester,
        String courseDescription,
        List<Integer> clusters
) {}

