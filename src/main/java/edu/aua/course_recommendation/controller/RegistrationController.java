package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.request.RegistrationRequestDto;
import edu.aua.course_recommendation.dto.response.RegistrationResponseDto;
import edu.aua.course_recommendation.dto.UserProfileDto;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.mappers.UserMapper;
import edu.aua.course_recommendation.mappers.UserRegistrationMapper;
import edu.aua.course_recommendation.repository.UserRepository;
import edu.aua.course_recommendation.service.auth.EmailVerificationService;
import edu.aua.course_recommendation.service.auth.OtpService;
import edu.aua.course_recommendation.service.auth.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    @Value("${email-verification.required}")
    private boolean emailVerificationRequired;

    private final UserRegistrationService userRegistrationService;

    private final UserRegistrationMapper userRegistrationMapper;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final OtpService otpService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

//    @PostMapping("/register")
//    public ResponseEntity<RegistrationResponseDto> registerUser(
//            @Valid @RequestBody final RegistrationRequestDto registrationRequestDto) {
//
//        final var registeredUser = userRegistrationService
//                .registerUser(userRegistrationMapper.toEntity(registrationRequestDto));
//
//        if (emailVerificationRequired) {
//            emailVerificationService.sendVerificationToken(registeredUser.getId(), registeredUser.getEmail());
//        }
//
//        return ResponseEntity.ok(
//                userRegistrationMapper.toRegistrationResponseDto(registeredUser, emailVerificationRequired)
//        );
//    }

    @PostMapping("/password-setup/request")
    public ResponseEntity<Void> requestPasswordSetup(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found for the provided email"));

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is already verified");
        }

        // Reuse the email service to send the password setup email
        // (You may modify the email subject/text if needed.)
        emailVerificationService.sendVerificationToken(user.getId(), user.getEmail());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-setup/confirm")
    public ResponseEntity<Void> confirmPasswordSetup(
            @RequestParam UUID uid,
            @RequestParam String t,  // the OTP token
            @RequestParam String newPassword) {

        // Validate the OTP using your existing OTP service
        if (!otpService.isOtpValid(uid, t)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        // Retrieve the user and update the password
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true);
        userRepository.save(user);

        // Delete the OTP token
        otpService.deleteOtp(uid);

        return ResponseEntity.noContent().build();
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
