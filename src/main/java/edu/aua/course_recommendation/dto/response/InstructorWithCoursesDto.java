package edu.aua.course_recommendation.dto.response;

import java.util.List;
import java.util.UUID;

public record InstructorWithCoursesDto(
    UUID id,
    String name,
    String imageUrl,
    String position,
    String mobile,
    String email,
    String bio,
    String officeLocation,
    List<CourseOfferingResponseDto> courseOfferings
) {}