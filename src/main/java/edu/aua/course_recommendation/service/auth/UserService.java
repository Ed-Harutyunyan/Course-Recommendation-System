package edu.aua.course_recommendation.service.auth;

import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.GONE;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserByUsername(final String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(GONE, "User has been deactivated or deleted"));
    }

    // Returns the currently authenticated user
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        // By default, principal is a Jwt when using the Spring Security OAuth2 Resource Server
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            // Typically, 'sub' is the username or unique identifier
            String username = jwt.getClaimAsString("sub");
            // Fetch the user from the database using the username
            return userRepository.findByUsername(username).orElse(null);
        }

        return null;
    }
}
