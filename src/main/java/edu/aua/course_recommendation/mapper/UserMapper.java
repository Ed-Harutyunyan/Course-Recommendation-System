package edu.aua.course_recommendation.mapper;

import edu.aua.course_recommendation.dto.UserProfileDto;
import edu.aua.course_recommendation.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserProfileDto toUserProfileDto(final User user) {
        return new UserProfileDto(user.getEmail(), user.getUsername(), user.getRole());
    }
}
