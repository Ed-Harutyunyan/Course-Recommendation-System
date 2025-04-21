package edu.aua.course_recommendation.mappers;

import edu.aua.course_recommendation.dto.EnrollmentDto;
import edu.aua.course_recommendation.dto.UserProfileDto;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.service.auth.UserService;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    private final UserService userService;
    private final CourseMapper courseMapper;

    public UserMapper(UserService userService, CourseMapper courseMapper) {
        this.userService = userService;
        this.courseMapper = courseMapper;
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

        // Who’s calling?
        User me = userService.getCurrentUser();
        boolean isAdmin = me != null && me.isAdmin();
        boolean isSelf = me != null && me.getId().equals(user.getId());

        if (isSelf || isAdmin) {
            var list = user.getEnrollments().stream().map(e -> {
                var ed = new EnrollmentDto();
                ed.setCourse(courseMapper.toCourseResponseDto(e.getCourse()));
                ed.setGrade(e.getGrade());
                ed.setYear(e.getYear());
                ed.setSemester(e.getSemester());
                return ed;
            }).toList();
            dto.setEnrollments(list);
        }
        return dto;
    }
}
