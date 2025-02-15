package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public User getUser() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User user
                ? user
                : null;
    }
}
