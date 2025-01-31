package edu.aua.course_recommendation.mapper;

import edu.aua.course_recommendation.dto.RegistrationRequestDto;
import edu.aua.course_recommendation.dto.RegistrationResponseDto;
import edu.aua.course_recommendation.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationMapper {

    public User toEntity(RegistrationRequestDto registrationRequestDto) {
        final var user = new User();

        user.setEmail(registrationRequestDto.email());
        user.setUsername(registrationRequestDto.username());
        user.setPassword(registrationRequestDto.password());

        return user;
    }

    public RegistrationResponseDto toRegistrationResponseDto(final User user) {
        return new RegistrationResponseDto(
                user.getUsername(), user.getEmail());
    }
}
