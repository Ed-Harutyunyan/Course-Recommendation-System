package edu.aua.course_recommendation.dto;

import java.util.List;

public record CourseDto(
        String courseCode,
        String courseTitle,
        String courseDescription,
        String prerequisites,
        String credits,
        List<Integer> themes
) {}
