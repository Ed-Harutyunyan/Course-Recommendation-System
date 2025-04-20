package edu.aua.course_recommendation.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserProfileDto {
    private String username;
    private String role;
    private String department;
    private String academicStanding;
    private String email;
    private String profilePictureUrl;

    // only populated if the viewer is the same as the profile
    private List<EnrollmentDto> enrollments;
}
