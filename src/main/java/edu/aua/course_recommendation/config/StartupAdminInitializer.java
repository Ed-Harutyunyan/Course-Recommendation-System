package edu.aua.course_recommendation.config;

import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.Department;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class StartupAdminInitializer {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    CommandLineRunner initAdminUser(UserRepository userRepository) {
        return args -> {
            createUserIfNotFound(userRepository, "admin", "admin@example.com", "admin", Department.CS, Role.ROLE_ADMIN, true, "Admin");
            createUserIfNotFound(userRepository, "tariel", "tariel_hakobyan@edu.aua.am", "tariel", Department.CS, Role.ROLE_STUDENT, false, "User");
            createUserIfNotFound(userRepository, "business", "business@edu.aua.am", "business", Department.BAB, Role.ROLE_STUDENT, true, "Business User");
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
}