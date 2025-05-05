package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.entity.*;
import edu.aua.course_recommendation.exceptions.AuthenticationException;
import edu.aua.course_recommendation.exceptions.CourseNotFoundException;
import edu.aua.course_recommendation.exceptions.EnrollmentException;
import edu.aua.course_recommendation.model.AcademicStanding;
import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.repository.CourseRepository;
import edu.aua.course_recommendation.repository.EnrollmentRepository;
import edu.aua.course_recommendation.repository.UserRepository;
import edu.aua.course_recommendation.service.auth.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final ThreadLocal<Set<String>> simulatedCompletedCourses = new ThreadLocal<>();

    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional //TODO: Should make grade mandatory and also include year and semester of the enrollment
    public void enroll(final UUID studentId, final String courseCode, String grade) {
        StudentAndCourse studentAndCourse = validateAndFetch(studentId, courseCode);
        User student = studentAndCourse.getStudent();
        Course course = studentAndCourse.getCourse();

        if (enrollmentRepository.existsByUserAndCourse(student, course)) {
            throw new EnrollmentException("You are already enrolled in this course");
        }

        // Make the composite key
        EnrollmentId enrollmentId = new EnrollmentId();
        enrollmentId.setUserId(studentId);
        enrollmentId.setCourseId(course.getId());

        // Create the enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentId);
        enrollment.setGrade(grade);
        enrollment.setUser(student);
        enrollment.setCourse(course);

        enrollmentRepository.save(enrollment);
        calculateAcademicStanding(studentId);
    }

    // Overloaded method for enrolling without providing a grade
    // Not providing a grade sets it to N/A
    @Transactional
    public void enroll(final UUID studentId, final String courseCode) {
        enroll(studentId, courseCode, "N/A");
    }

    @Transactional
    public void enrollAll(UUID studentId) {
        List<Course> courses = courseRepository.findAll();
        for (Course course : courses) {
            StudentAndCourse studentAndCourse = validateAndFetch(studentId, course.getCode());
            User student = studentAndCourse.getStudent();
            Course validCourse = studentAndCourse.getCourse();

            if (enrollmentRepository.existsByUserAndCourse(student, validCourse)) {
                log.info("Skipping: You are already enrolled in this course: {}", validCourse.getCode());
            }

            // Make the composite key
            EnrollmentId enrollmentId = new EnrollmentId();
            enrollmentId.setUserId(studentId);
            enrollmentId.setCourseId(validCourse.getId());

            // Create the enrollment
            Enrollment enrollment = new Enrollment();
            enrollment.setId(enrollmentId);
            enrollment.setGrade("N/A");
            enrollment.setUser(student);
            enrollment.setCourse(course);

            enrollmentRepository.save(enrollment);
        }
        calculateAcademicStanding(studentId);
    }

    @Transactional
    public void drop(UUID studentId, String courseCode) {
        StudentAndCourse studentAndCourse = validateAndFetch(studentId, courseCode);
        User student = studentAndCourse.getStudent();
        Course course = studentAndCourse.getCourse();

        if (!enrollmentRepository.existsByUserAndCourse(student, course)) {
            throw new EnrollmentException("You are not enrolled in this course");
        }

        enrollmentRepository.deleteByUserAndCourse(student, course);
        calculateAcademicStanding(studentId);
    }

    // This returns all the course *codes* that the student didn't get a
    // W or F grade in
    @Transactional(readOnly = true)
    public List<String> getCompletedCourseCodes(UUID studentId) {
        List<String> actual = enrollmentRepository.findByUser_Id(studentId).stream()
                .filter(e -> isPassingGrade(e.getGrade()))
                .map(e -> e.getCourse().getCode())
                .toList();

        Set<String> simulated = simulatedCompletedCourses.get();
        if (simulated != null) {
            Set<String> union = new HashSet<>(actual);
            union.addAll(simulated);
            return new ArrayList<>(union);
        }

        return actual;
    }

    // HELPERS
    public void setSimulatedCompletedCourses(Set<String> simulated) {
        simulatedCompletedCourses.set(simulated);
    }

    public void clearSimulatedCompletedCourses() {
        simulatedCompletedCourses.remove();
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
    private StudentAndCourse validateAndFetch(UUID studentId, String courseCode) {
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
        Course course = courseRepository.findByCode(courseCode)
                .orElseThrow(() -> new CourseNotFoundException("Course with code " + courseCode + " not found"));

        return new StudentAndCourse(authenticatedUser, course);
    }

    public List<Enrollment> getEnrollments(UUID studentId) {
        return enrollmentRepository.findByUser_Id(studentId);
    }

    public List<Course> getCourses(UUID studentId) {
        return enrollmentRepository.findByUser_Id(studentId).stream()
                .map(Enrollment::getCourse)
                .toList();
    }

    public List<Course> getCompletedCourses(UUID studentId) {
        return enrollmentRepository.findByUser_Id(studentId).stream()
                .filter(e -> isPassingGrade(e.getGrade()))
                .map(Enrollment::getCourse)
                .toList();
    }

    public AcademicStanding calculateAcademicStanding(UUID studentId) {
        int totalCredits = getCompletedCourses(studentId).stream()
                .mapToInt(Course::getCredits)
                .sum();

        AcademicStanding standing = AcademicStanding.getStandingFromCredits(totalCredits);

        // Update the user's academic standing
        User student = userService.getCurrentUser();
        student.setAcademicStanding(standing);
        userRepository.save(student);

        return standing;
    }

    @Transactional
    public void enrollList(UUID studentId, List<String> courseCodes) {
        for (String code : courseCodes) {
            enroll(studentId, code);
        }
    }

    public void dropAll(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUser_Id(studentId);
        enrollmentRepository.deleteAll(enrollments);
        calculateAcademicStanding(studentId);
    }

    public void dropAllEnrollments() {
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        enrollmentRepository.deleteAll(enrollments);
        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getUser();
            calculateAcademicStanding(student.getId());
        }
    }

    @RequiredArgsConstructor
    @Getter @Setter
    private static class StudentAndCourse {
        private final User student;
        private final Course course;
    }
}
