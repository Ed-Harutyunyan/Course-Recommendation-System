package edu.aua.course_recommendation.mappers;

import edu.aua.course_recommendation.dto.response.UserProfileDto;
import edu.aua.course_recommendation.entity.Enrollment;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.repository.EnrollmentRepository;
import edu.aua.course_recommendation.service.auth.UserService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {
    private final UserService userService;
    private final CourseMapper courseMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;

    public UserMapper(UserService userService, CourseMapper courseMapper,
                      EnrollmentRepository enrollmentRepository, EnrollmentMapper enrollmentMapper) {
        this.userService = userService;
        this.courseMapper = courseMapper;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentMapper = enrollmentMapper;
    }

    public UserProfileDto toUserProfileDto(User user) {
        // Basic public info
        var dto = new UserProfileDto();
        dto.setId(user.getId().toString());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole().name());
        dto.setDepartment(user.getDepartment().name());
        dto.setAcademicStanding(user.getAcademicStanding().name());
        dto.setEmail(user.getEmail());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());

        // Who's calling?
        User me = userService.getCurrentUser();
        boolean isAdmin = me != null && me.isAdmin();
        boolean isSelf = me != null && me.getId().equals(user.getId());

        if (isSelf || isAdmin) {
            // Explicitly fetch enrollments instead of using user.getEnrollments()
            List<Enrollment> enrollments = enrollmentRepository.findByUser_Id(user.getId());
            dto.setEnrollments(enrollmentMapper.toResponseDtoList(enrollments));
        }
        return dto;
    }
}