package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.entity.*;
import edu.aua.course_recommendation.exceptions.AuthenticationException;
import edu.aua.course_recommendation.exceptions.CourseOfferingNotFoundException;
import edu.aua.course_recommendation.exceptions.EnrollmentException;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.repository.CourseOfferingRepository;
import edu.aua.course_recommendation.repository.CourseRepository;
import edu.aua.course_recommendation.repository.EnrollmentRepository;
import edu.aua.course_recommendation.service.auth.UserService;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public void enroll(final UUID studentId, final UUID courseId, String grade) {
        StudentAndCourse studentAndCourse = validateAndFetch(studentId, courseId);
        User student = studentAndCourse.getStudent();
        Course course = studentAndCourse.getCourse();

        if (enrollmentRepository.existsByUserAndCourse(student, course)) {
            throw new EnrollmentException("You are already enrolled in this course");
        }

        // Make the composite key
        EnrollmentId enrollmentId = new EnrollmentId();
        enrollmentId.setUserId(studentId);
        enrollmentId.setCourseId(courseId);

        // Create the enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentId);
        enrollment.setGrade(grade);
        enrollment.setUser(student);
        enrollment.setCourse(course);

        enrollmentRepository.save(enrollment);
    }

    // Overloaded method for enrolling without providing a grade
    // Not providing a grade sets it to N/A
    @Transactional
    public void enroll(final UUID studentId, final UUID courseId) {
        enroll(studentId, courseId, "N/A");
    }

    @Transactional
    public void drop(UUID studentId, UUID courseOfferingId) {
        StudentAndCourse studentAndCourse = validateAndFetch(studentId, courseOfferingId);
        User student = studentAndCourse.getStudent();
        Course course = studentAndCourse.getCourse();

        if (!enrollmentRepository.existsByUserAndCourse(student, course)) {
            throw new EnrollmentException("You are not enrolled in this course");
        }

        enrollmentRepository.deleteByUserAndCourse(student, course);
    }

    // This returns all the course *codes* that the student didn't get a
    // W or F grade in
    public List<String> getCompletedCourseCodes(UUID studentId) {
        return enrollmentRepository.findByUser_Id(studentId).stream()
                .filter(e -> isPassingGrade(e.getGrade()))
                .map(e -> e.getCourse().getCode())
                .toList();
    }

    // By default, each grade is set to "N/A"
    // So the hardcoded logic of NOT "F" and NOT "W" is used to determine if a grade is passing
    // Might be better to just assume each course selected by student is already passed
    // And id like to change grade to be just optional and later add a GPA calculator to the system
    private boolean isPassingGrade(String grade) {
        if (grade == null) {
            return true; // No grade provided means the course is passed
        }

        return !"F".equalsIgnoreCase(grade) && !"W".equalsIgnoreCase(grade);
    }


    // Validates the authenticated user against the provided studentId and fetches the course.
    private StudentAndCourse validateAndFetch(UUID studentId, UUID courseId) {
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
        Course course = courseRepository.findCourseById(courseId)
                .orElseThrow(() -> new CourseOfferingNotFoundException("Course offering not found"));

        return new StudentAndCourse(authenticatedUser, course);
    }

    @RequiredArgsConstructor
    @Getter @Setter
    private static class StudentAndCourse {
        private final User student;
        private final Course course;
    }
}
