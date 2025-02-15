package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.Enrollment;
import edu.aua.course_recommendation.entity.Student;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.repository.CourseRepository;
import edu.aua.course_recommendation.repository.EnrollmentRepository;
import edu.aua.course_recommendation.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final UserService userService;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public void enroll(final UUID studentId, final UUID courseId) {
        User authenticatedUser = userService.getUser();

        if (authenticatedUser == null) {
            throw new IllegalArgumentException("You are not authenticated");
        }

        // This checks if the person sends the request is indeed the authenticated user
        // Maybe there is a better way to do this check though?
        if (!authenticatedUser.getId().equals(studentId)) {
            throw new IllegalArgumentException("You can only enroll yourself");
        }

        if (authenticatedUser.getRole() != Role.ROLE_STUDENT) {
            throw new IllegalArgumentException("Only students can enroll in courses");
        }

        Student student = studentRepository.findStudentById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Course course = courseRepository.findCourseById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
            throw new IllegalArgumentException("You are already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);

        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void drop(UUID studentId, UUID courseId) {
    }
}
