package edu.aua.course_recommendation.config;

import edu.aua.course_recommendation.service.course.EnrollmentService;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.Department;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class StartupAdminInitializer {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    CommandLineRunner initAdminUser(UserRepository userRepository,
                                    EnrollmentService enrollmentService) {
        return args -> {
            createUserIfNotFound(userRepository, "admin", "admin@example.com", "admin", Department.CS, Role.ROLE_ADMIN, true, "Admin");
            createUserIfNotFound(userRepository, "tariel", "tariel_hakobyan@edu.aua.am", "tariel", Department.CS, Role.ROLE_STUDENT, false, "User");
            createUserIfNotFound(userRepository, "business", "business@edu.aua.am", "business", Department.BAB, Role.ROLE_STUDENT, true, "Business User");
            createUserIfNotFound(userRepository, "cs", "cs@edu.aua.am", "cs", Department.CS, Role.ROLE_STUDENT, true, "CS User");

            // Example usage:
            // First semester
            //enrollStudentIn("cs", List.of("PEER001", "FND152", "FND153", "FND110", "FND101", "CS100", "CS111", "CS110", "CHSS110"), userRepository, enrollmentService);

            // Second semester
            //enrollStudentIn("cs", List.of(), userRepository, enrollmentService);
        };
    }

    private void createUserIfNotFound(UserRepository userRepository, String username, String email, String rawPassword,
                                      Department department, Role role, boolean emailVerified, String userType) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setDepartment(department);
            user.setRole(role);
            user.setEmailVerified(emailVerified);

            userRepository.save(user);
            System.out.printf("✅ Default %s User Created: %s/%s%n", userType, username, rawPassword);
        } else {
            System.out.printf("✅ %s user '%s' already exists.%n", userType, username);
        }
    }

    private void enrollStudentIn(String username, List<String> courseCodes, UserRepository userRepository,
                                 EnrollmentService enrollmentService) {
        // Find the student by username
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + username));

        // Verify the user is a student
        if (student.getRole() != Role.ROLE_STUDENT) {
            throw new IllegalArgumentException("User is not a student: " + username);
        }

        // Enroll the student in each course
        for (String code : courseCodes) {
            try {
                // Use the existing service method to handle enrollment
                enrollmentService.enroll(student.getId(), code, "A", "202425", "1");
                System.out.printf("✅ Enrolled student '%s' in course '%s'%n", username, code);
            } catch (Exception e) {
                System.out.printf("❌ Failed to enroll student '%s' in course '%s': %s%n",
                        username, code, e.getMessage());
            }
        }
    }
}

