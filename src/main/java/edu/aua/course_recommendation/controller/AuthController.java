package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.UserProfileDto;
import edu.aua.course_recommendation.dto.request.AuthenticationRequestDto;
import edu.aua.course_recommendation.dto.response.AuthenticationResponseDto;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.mappers.UserMapper;
import edu.aua.course_recommendation.repository.UserRepository;
import edu.aua.course_recommendation.service.auth.AuthenticationService;
import edu.aua.course_recommendation.service.auth.EmailVerificationService;
import edu.aua.course_recommendation.service.auth.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static edu.aua.course_recommendation.model.AuthTokens.REFRESH_TOKEN_COOKIE_NAME;
import static edu.aua.course_recommendation.util.CookieUtil.addCookie;
import static edu.aua.course_recommendation.util.CookieUtil.removeCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final UserMapper userMapper;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDto> authenticate(
            @RequestBody final AuthenticationRequestDto authenticationRequestDto) {
        final var authTokens = authenticationService.authenticate(authenticationRequestDto.username(), authenticationRequestDto.password());

        return ResponseEntity.ok()
                .header(SET_COOKIE, addCookie(REFRESH_TOKEN_COOKIE_NAME, authTokens.refreshToken(), authTokens.refreshTokenTtl()).toString())
                .body(new AuthenticationResponseDto(authTokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponseDto> refresh(@CookieValue(REFRESH_TOKEN_COOKIE_NAME) final String refreshToken) {
        final var authTokens = authenticationService.refreshToken(refreshToken);

        return ResponseEntity.ok(new AuthenticationResponseDto(authTokens.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> revokeToken(@CookieValue(REFRESH_TOKEN_COOKIE_NAME) final String refreshToken) {
        authenticationService.revokeRefreshToken(refreshToken);
        System.out.println("Request for logging out: " + refreshToken);
        return ResponseEntity.noContent()
                .header(SET_COOKIE, removeCookie(REFRESH_TOKEN_COOKIE_NAME).toString())
                .build();
    }

    @PostMapping("/password-setup/request")
    public ResponseEntity<Void> requestPasswordSetup(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found for the provided email"));

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is already verified");
        }

        emailVerificationService.sendVerificationToken(user.getId(), user.getEmail(), user.getUsername());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-setup/reset")
    public ResponseEntity<Void> resetPassword(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found for the provided email"));

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account not yet verified. Please sign-up first.");
        }

        emailVerificationService.sendPasswordReset(user.getId(), user.getEmail(), user.getUsername());

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
