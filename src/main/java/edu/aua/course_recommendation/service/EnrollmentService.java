package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.Enrollment;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.repository.CourseRepository;
import edu.aua.course_recommendation.repository.EnrollmentRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final UserService userService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public void enroll(final UUID studentId, final UUID courseId) {
        StudentAndCourse studentAndCourse = validateAndFetch(studentId, courseId);
        User student = studentAndCourse.getStudent();
        Course course = studentAndCourse.getCourse();

        if (enrollmentRepository.existsByUserAndCourse(student, course)) {
            throw new IllegalArgumentException("You are already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUser(student);
        enrollment.setCourse(course);

        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void drop(UUID studentId, UUID courseId) {
        StudentAndCourse studentAndCourse = validateAndFetch(studentId, courseId);
        User student = studentAndCourse.getStudent();
        Course course = studentAndCourse.getCourse();

        if (!enrollmentRepository.existsByUserAndCourse(student, course)) {
            throw new IllegalArgumentException("You are not enrolled in this course");
        }

        enrollmentRepository.deleteByUserAndCourse(student, course);
    }

    // Validates the authenticated user against the provided studentId and fetches the course.
    private StudentAndCourse validateAndFetch(UUID studentId, UUID courseId) {
        User authenticatedUser = userService.getUser();
        if (authenticatedUser == null) {
            throw new IllegalArgumentException("You are not authenticated");
        }
        if (!authenticatedUser.getId().equals(studentId)) {
            throw new IllegalArgumentException("You can only enroll yourself");
        }
        if (authenticatedUser.getRole() != Role.ROLE_STUDENT) {
            throw new IllegalArgumentException("Only students can enroll in courses");
        }
        Course course = courseRepository.findCourseById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        return new StudentAndCourse(authenticatedUser, course);
    }

    @RequiredArgsConstructor
    @Getter
    @Setter
    private static class StudentAndCourse {
        private final User student;
        private final Course course;
    }
}
