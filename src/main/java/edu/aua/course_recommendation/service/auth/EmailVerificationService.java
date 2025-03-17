package edu.aua.course_recommendation.service.auth;

import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final OtpService otpService;

    private final UserRepository userRepository;

    private final JavaMailSender mailSender;

    @Async
    public void sendVerificationToken(UUID userId, String email) {
        final var token = otpService.generateAndStoreOtp(userId);

        final var emailVerificationUrl =
                "http://localhost:8080/api/auth/email/verify?uid=%s&t=%s".formatted(userId, token);

        final var text = "Click here to verify your account: " + emailVerificationUrl;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Email Verification");
        message.setText(text);
        message.setFrom("support@gmail.com");

        mailSender.send(message);
    }

    public void resendVerificationToken(String email) {
        User user = userRepository.findByEmail(email)
                .filter(u -> !u.isEmailVerified())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Email not found or account is already verified"));

        sendVerificationToken(user.getId(), user.getEmail());
    }

    @Transactional
    public User verifyEmail(UUID userId, String token) {
        if (!otpService.isOtpValid(userId, token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or Expired OTP");
        }

        otpService.deleteOtp(userId);

        final var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already verified");
        }

        user.setEmailVerified(true);

        return user;
    }
}
