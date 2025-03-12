package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.exceptions.EmailVerificationException;
import edu.aua.course_recommendation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    @Value("${email-verification.required}")
    private boolean emailVerificationRequired;

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).map(user -> {
            if (emailVerificationRequired && !user.isEmailVerified()) {
                Map<String, String> errors = Map.of("email", "Email is not verified");
                throw new EmailVerificationException(HttpStatus.FORBIDDEN, errors);
        }
        return User.builder()
                .username(username)
                .password(user.getPassword())
                .build();
        })
        .orElseThrow(() -> new UsernameNotFoundException("User with name " + username + " not found"));
    }
}
