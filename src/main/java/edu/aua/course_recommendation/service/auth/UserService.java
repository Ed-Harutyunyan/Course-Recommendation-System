package edu.aua.course_recommendation.service.auth;

import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.AcademicStanding;
import edu.aua.course_recommendation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.GONE;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUserByEmail(final String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(GONE, "User has been deactivated or deleted"));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(final String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(GONE, "User has been deactivated or deleted"));
    }

    @Transactional(readOnly = true)
    public User getUserById(final UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(GONE, "User has been deactivated or deleted"));
    }

    // Returns the currently authenticated user
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        // By default, principal is a Jwt when using the Spring Security OAuth2 Resource Server
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("sub");
            return userRepository.findByEmail(email).orElse(null);
        }

        return null;
    }

    @Transactional(readOnly = true)
    public AcademicStanding getAcademicStanding(UUID studentId) {
        return getUserById(studentId).getAcademicStanding();
    }

    @Transactional
    public void validateStudent(UUID studentId) {
        User student = getUserById(studentId);
        User currentUser = getCurrentUser();

        if (student == null) {
            throw new IllegalArgumentException("Student not found");
        }

        if (!student.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You are not authorized to audit this student");
        }
    }

    @Transactional
    public void deactivateUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(GONE, "User not found"));

        user.setEmailVerified(false);
        userRepository.save(user);
    }
}
