package edu.aua.course_recommendation.dto.response;

public record EnrollmentResponseDto(
        CourseInfo course,
        String grade,
        String year,
        String semester
) {
    // Nested record for course information
    public record CourseInfo(
            String courseCode,
            String courseTitle,
            Integer courseCredits
    ) {}
}