package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.exception.ValidationException;
import edu.aua.course_recommendation.model.User;
import edu.aua.course_recommendation.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user) {
        final var errors = new HashMap<String, String>();

        if (userRepository.existsByUsername(user.getUsername())) {
            errors.put("username", "The username " + user.getUsername() + " is already in use");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            errors.put("email", "The email " + user.getEmail() + " is already in use");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(HttpStatus.CONFLICT, errors);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }
}
