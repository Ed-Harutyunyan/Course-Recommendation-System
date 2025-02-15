package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.InstructorProfile;
import edu.aua.course_recommendation.entity.StudentProfile;
import edu.aua.course_recommendation.exception.ValidationException;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.repository.InstructorProfileRepository;
import edu.aua.course_recommendation.repository.StudentProfileRepository;
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
    private final StudentProfileRepository studentProfileRepository;
    private final InstructorProfileRepository instructorProfileRepository;

    @Transactional
    public User registerUser(User user) {
        final var errors = new HashMap<String, String>();

        if (userRepository.existsByUsername(user.getUsername())) {
            errors.put("username", "The username " + user.getUsername() + " is already in use");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            errors.put("email", "The email " + user.getEmail() + " is already in use");
        }

        try {
            Role role = determineUserRole(user.getEmail());
            user.setRole(role);
        } catch (IllegalArgumentException exception) {
            errors.put("email", exception.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(HttpStatus.CONFLICT, errors);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        Role userRole = savedUser.getRole();
        if (userRole == Role.ROLE_STUDENT || userRole == Role.ROLE_ALUMNI) {
            StudentProfile studentProfile = new StudentProfile();
            studentProfile.setUser(savedUser);
            studentProfileRepository.save(studentProfile);
        } else if (userRole == Role.ROLE_INSTRUCTOR) {
            InstructorProfile instructorProfile = new InstructorProfile();
            instructorProfile.setUser(savedUser);
            instructorProfileRepository.save(instructorProfile);
        }

        return savedUser;
    }

    /**
     * Determines the role based on the email domain.
     * Allowed domains:
     * - @edu.aua.am → STUDENT
     * - @alumni.aua.am → ALUMNI
     * - @aua.am → PROFESSOR
     */
    private Role determineUserRole(String email) {
        String lowerCaseEmail = email.toLowerCase();

        if (lowerCaseEmail.endsWith("@edu.aua.am")) {
            return Role.ROLE_STUDENT;
        } else if (lowerCaseEmail.endsWith("@alumni.aua.am")) {
            return Role.ROLE_ALUMNI;
        } else if (lowerCaseEmail.endsWith("@aua.am")) {
            return Role.ROLE_INSTRUCTOR;
        } else {
            throw new IllegalArgumentException("Email domain not allowed. Allowed domains are edu.aua.am, alumni.aua.am, and aua.am.");
        }
    }
}
