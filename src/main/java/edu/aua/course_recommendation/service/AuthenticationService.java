package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.RefreshToken;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.AuthTokens;
import edu.aua.course_recommendation.repository.RefreshTokenRepository;
import edu.aua.course_recommendation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.between;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthTokens authenticate(final String username, final String password) {
        final var authToken = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        final var authentication = authenticationManager.authenticate(authToken);

        final var user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User with username [%s] not found".formatted(username)));

        return authenticate(user);
    }

    public AuthTokens authenticate(final User user) {
        final var accessToken = jwtService.generateToken(user.getUsername(), user.getRole().name());

        final var refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15))); // TODO: Update hardcoded value, 15 minutes
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthTokens(accessToken, refreshTokenEntity.getId().toString(), between(Instant.now(), refreshTokenEntity.getExpiresAt()));
    }

    public AuthTokens refreshToken(final String refreshToken) {
        final var refreshTokenEntity = refreshTokenRepository.findByIdAndExpiresAtAfter(validateRefreshTokenFormat(refreshToken), Instant.now())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        final var newAccessToken = jwtService.generateToken(refreshTokenEntity.getUser().getUsername(), refreshTokenEntity.getUser().getRole().name());

        return new AuthTokens(newAccessToken, refreshToken, between(Instant.now(), refreshTokenEntity.getExpiresAt()));
    }

    public void revokeRefreshToken(String refreshToken) {
        System.out.println("Revoking refresh token" + refreshToken);
        refreshTokenRepository.deleteById(validateRefreshTokenFormat(refreshToken));
    }

    private UUID validateRefreshTokenFormat(final String refreshToken) {
        try {
            return UUID.fromString(refreshToken);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
    }


}
