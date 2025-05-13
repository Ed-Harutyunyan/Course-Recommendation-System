package edu.aua.course_recommendation.dto.response;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record DetailedCourseResponseDto(
        UUID courseId,
        String courseCode,
        String courseTitle,
        String courseDescription,
        Integer courseCredits,
        Set<String> coursePrerequisites,
        List<Integer> courseThemes
) {
}
