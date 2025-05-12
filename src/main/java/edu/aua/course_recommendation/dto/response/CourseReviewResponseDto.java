package edu.aua.course_recommendation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseReviewResponseDto(
        UUID id,
        UUID userId,
        String userName,
        UUID courseId,
        String courseCode,
        String content,
        int rating,
        LocalDateTime createdAt,
        String profilePictureUrl
) {}
