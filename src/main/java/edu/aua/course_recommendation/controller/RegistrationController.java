package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.RegistrationRequestDto;
import edu.aua.course_recommendation.dto.RegistrationResponseDto;
import edu.aua.course_recommendation.mapper.UserRegistrationMapper;
import edu.aua.course_recommendation.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserRegistrationService userRegistrationService;

    private final UserRegistrationMapper userRegistrationMapper;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> registerUser(
            @Valid @RequestBody final RegistrationRequestDto registrationRequestDto) {

        final var registeredUser = userRegistrationService
                .registerUser(registrationRequestDto);

        return ResponseEntity.ok(
                userRegistrationMapper.toRegistrationResponseDto(registeredUser)
        );
    }
}
