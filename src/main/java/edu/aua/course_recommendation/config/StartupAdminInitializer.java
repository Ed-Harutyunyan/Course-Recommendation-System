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
            String adminUsername = "admin";
            String adminEmail = "admin@example.com";

            if (userRepository.findByUsername(adminUsername).isEmpty()) {
                User adminUser = new User();
                adminUser.setUsername(adminUsername);
                adminUser.setEmail(adminEmail);
                adminUser.setPassword(passwordEncoder.encode("admin"));
                adminUser.setDepartment(Department.CS);
                adminUser.setRole(Role.ROLE_ADMIN);
                adminUser.setEmailVerified(true);

                userRepository.save(adminUser);
                System.out.println("✅ Default Admin User Created: admin/admin");
            } else {
                System.out.println("✅ Admin user already exists.");
            }
        };
    }
}
