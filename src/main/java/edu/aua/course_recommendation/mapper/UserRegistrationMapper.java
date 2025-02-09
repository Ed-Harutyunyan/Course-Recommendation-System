package edu.aua.course_recommendation.mapper;

import edu.aua.course_recommendation.dto.request.RegistrationRequestDto;
import edu.aua.course_recommendation.dto.response.RegistrationResponseDto;
import edu.aua.course_recommendation.entity.User;
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

    public RegistrationResponseDto toRegistrationResponseDto(final User user, boolean emailVerificationRequired) {
        return new RegistrationResponseDto(
                user.getUsername(), user.getEmail(), emailVerificationRequired);
    }
}
