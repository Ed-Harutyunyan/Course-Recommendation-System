package edu.aua.course_recommendation.dto;

import java.util.List;
import java.util.Set;

public record DetailedCourseResponseDto(
        String courseCode,
        String courseTitle,
        String courseDescription,
        Integer courseCredits,
        Set<String> coursePrerequisites,
        List<Integer> courseThemes
) {
}
