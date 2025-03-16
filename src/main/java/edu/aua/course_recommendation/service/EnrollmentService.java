package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.*;
import edu.aua.course_recommendation.exceptions.AuthenticationException;
import edu.aua.course_recommendation.exceptions.CourseOfferingNotFoundException;
import edu.aua.course_recommendation.exceptions.EnrollmentException;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.repository.CourseOfferingRepository;
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
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public void enroll(final UUID studentId, final UUID courseOfferingId) {
        StudentAndCourseOffering studentAndCourse = validateAndFetch(studentId, courseOfferingId);
        User student = studentAndCourse.getStudent();
        CourseOffering courseOffering = studentAndCourse.getCourseOffering();

        System.out.println("Student: " + student.getId());
        System.out.println("CourseOffering: " + courseOffering.getId());

        if (enrollmentRepository.existsByUserAndCourseOffering(student, courseOffering)) {
            throw new EnrollmentException("You are already enrolled in this course");
        }

        // This Composite Key should've been auto-generated by Hibernate
        // But for some reason isn't so, keep this to make it work...
        EnrollmentId enrollmentId = new EnrollmentId();
        enrollmentId.setUserId(studentId);
        enrollmentId.setCourseOfferingId(courseOfferingId);

        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentId);
        enrollment.setGrade("N/A");
        enrollment.setUser(student);
        enrollment.setCourseOffering(courseOffering);

        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void drop(UUID studentId, UUID courseOfferingId) {
        StudentAndCourseOffering studentAndCourse = validateAndFetch(studentId, courseOfferingId);
        User student = studentAndCourse.getStudent();
        CourseOffering courseOffering = studentAndCourse.getCourseOffering();

        if (!enrollmentRepository.existsByUserAndCourseOffering(student, courseOffering)) {
            throw new EnrollmentException("You are not enrolled in this course");
        }

        enrollmentRepository.deleteByUserAndCourseOffering(student, courseOffering);
    }

    // Validates the authenticated user against the provided studentId and fetches the course.
    private StudentAndCourseOffering validateAndFetch(UUID studentId, UUID courseOfferingId) {
        User authenticatedUser = userService.getCurrentUser();
        if (authenticatedUser == null) {
            throw new AuthenticationException("No authenticated user found");
        }
        if (!authenticatedUser.getId().equals(studentId)) {
            throw new EnrollmentException("You can only enroll yourself");
        }
        if (authenticatedUser.getRole() != Role.ROLE_STUDENT) {
            throw new EnrollmentException("Only students can enroll in courses");
        }
        CourseOffering courseOffering = courseOfferingRepository.findCourseOfferingsById(courseOfferingId)
                .orElseThrow(() -> new CourseOfferingNotFoundException("Course offering not found"));

        return new StudentAndCourseOffering(authenticatedUser, courseOffering);
    }

    @RequiredArgsConstructor
    @Getter @Setter
    private static class StudentAndCourseOffering {
        private final User student;
        private final CourseOffering courseOffering;
    }
}
