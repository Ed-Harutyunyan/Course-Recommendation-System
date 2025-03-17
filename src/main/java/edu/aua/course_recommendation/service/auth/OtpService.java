package edu.aua.course_recommendation.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;

    public String generateAndStoreOtp(final UUID id) {
        final var otp = generateOtp();

        final var cacheKey = getCacheKey(id);

        redisTemplate.opsForValue().set(cacheKey, otp, Duration.ofMinutes(5));

        return otp;
    }

    public String getCacheKey(final UUID id) {
        return "otp:%s".formatted(id);
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int index = SECURE_RANDOM.nextInt("ABCDEFG123456789".length());
            otp.append("ABCDEFG123456789".charAt(index));
        }
        return otp.toString();
    }

    public void deleteOtp(final UUID id) {
        final var cacheKey = getCacheKey(id);
        redisTemplate.delete(cacheKey);
    }

    public boolean isOtpValid(final UUID id, final String otp) {
        final var cacheKey = getCacheKey(id);
        return Objects.equals(
                redisTemplate.opsForValue().get(cacheKey), otp);
    }
}
