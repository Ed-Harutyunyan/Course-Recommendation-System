package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.request.AuthenticationRequestDto;
import edu.aua.course_recommendation.dto.response.AuthenticationResponseDto;
import edu.aua.course_recommendation.service.auth.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static edu.aua.course_recommendation.model.AuthTokens.REFRESH_TOKEN_COOKIE_NAME;
import static edu.aua.course_recommendation.util.CookieUtil.addCookie;
import static edu.aua.course_recommendation.util.CookieUtil.removeCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

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
}
