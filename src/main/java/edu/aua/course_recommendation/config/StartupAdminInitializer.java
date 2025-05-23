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
            createUserIfNotFound(userRepository, "tariel", "tariel_hakobyan@edu.aua.am", "tariel", Department.CS, Role.ROLE_STUDENT, true, "User");
            createUserIfNotFound(userRepository, "business", "business@edu.aua.am", "business", Department.BAB, Role.ROLE_STUDENT, true, "Business User");
            createUserIfNotFound(userRepository, "cs", "cs@edu.aua.am", "cs", Department.CS, Role.ROLE_STUDENT, true, "CS User");

            // Example usage:
            // First semester
            enrollStudentIn("cs", List.of("PEER001", "FND152", "FND153", "FND110", "FND101", "CS100", "CS111", "CS110", "CHSS110",
                    "FND110BB", "FND102", "CS120", "CS101", "CS104", "CHSS111"), userRepository, enrollmentService);

            enrollStudentIn("tariel", List.of("PEER001", "FND152", "FND153", "FND110", "FND101", "CS100", "CS111", "CS110", "CHSS110",
                    "FND110BB", "FND102", "CS120", "CS101", "CS104", "CHSS111",
                    "PSIA201", "FND103", "CS121", "CS102", "CS130",
                    "FND104", "CS211", "CS103", "CS107", "FND110BBM", "FND110YO",
                    "FND221", "CS112", "CS108", "CHSS251", "CS222",
                    "CHSS282", "CS215", "CS236", "CS331",
                    "CHSS128", "CSE285", "CSE210", "CSE141", "CSE190", "CS213", "CS140",
                    "FND222", "CS290", "CS221", "DS116", "CS296"), userRepository, enrollmentService);

            System.out.println("CS Student Enrolled in First Semester CS Typical Courses");
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
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + username));

        if (student.getRole() != Role.ROLE_STUDENT) {
            throw new IllegalArgumentException("User is not a student: " + username);
        }

        int enrollmentCount = enrollmentService.getEnrollments(student.getId()).size();
        System.out.println("Student is currently enrolled in: " + enrollmentCount + " courses");

        enrollmentService.dropAll(student.getId());
        System.out.printf("Cleared existing enrollments for student '%s'%n", username);

        List<edu.aua.course_recommendation.dto.request.EnrollmentRequestDto> requests = courseCodes.stream()
                .map(code -> new edu.aua.course_recommendation.dto.request.EnrollmentRequestDto(
                        code, "A", "202425", "1"
                ))
                .toList();

        try {
            enrollmentService.enrollList(student.getId(), requests);
            System.out.printf("✅ Enrolled student '%s' in %d courses%n", username, courseCodes.size());
        } catch (Exception e) {
            System.out.printf("❌ Failed to enroll student '%s': %s%n", username, e.getMessage());
        }
    }
}

