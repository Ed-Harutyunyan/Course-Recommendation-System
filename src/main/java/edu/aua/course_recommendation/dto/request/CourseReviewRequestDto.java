package edu.aua.course_recommendation.dto.request;

import java.util.UUID;

public record CourseReviewRequestDto(
        UUID courseId,
        String content,
        int rating
) {}
