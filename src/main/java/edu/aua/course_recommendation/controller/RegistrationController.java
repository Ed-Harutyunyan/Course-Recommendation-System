package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.request.RegistrationRequestDto;
import edu.aua.course_recommendation.dto.response.RegistrationResponseDto;
import edu.aua.course_recommendation.dto.UserProfileDto;
import edu.aua.course_recommendation.mappers.UserMapper;
import edu.aua.course_recommendation.mappers.UserRegistrationMapper;
import edu.aua.course_recommendation.service.EmailVerificationService;
import edu.aua.course_recommendation.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    @Value("${email-verification.required}")
    private boolean emailVerificationRequired;

    private final UserRegistrationService userRegistrationService;

    private final UserRegistrationMapper userRegistrationMapper;
    private final EmailVerificationService emailVerificationService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> registerUser(
            @Valid @RequestBody final RegistrationRequestDto registrationRequestDto) {

        final var registeredUser = userRegistrationService
                .registerUser(userRegistrationMapper.toEntity(registrationRequestDto));

        if (emailVerificationRequired) {
            emailVerificationService.sendVerificationToken(registeredUser.getId(), registeredUser.getEmail());
        }

        return ResponseEntity.ok(
                userRegistrationMapper.toRegistrationResponseDto(registeredUser, emailVerificationRequired)
        );
    }

    @PostMapping("/email/verify")
    public ResponseEntity<UserProfileDto> verifyEmail(
            @RequestParam("uid") String userId, @RequestParam("t") String token
    ) {
        // String userId is taken then converted to UUID, kinda iffy
        final var verifiedUser = emailVerificationService.verifyEmail(UUID.fromString(userId), token);

        return ResponseEntity.ok(userMapper.toUserProfileDto(verifiedUser));
    }

    @PostMapping("/email/resend-verification")
    public ResponseEntity<Void> resendVerificationEmail(@RequestParam String email) {

        emailVerificationService.resendVerificationToken(email);
        return ResponseEntity.noContent().build();
    }
}
