package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.dto.request.EnrollmentRequestDto;
import edu.aua.course_recommendation.entity.*;
import edu.aua.course_recommendation.exceptions.CourseNotFoundException;
import edu.aua.course_recommendation.exceptions.EnrollmentException;
import edu.aua.course_recommendation.exceptions.UserNotFoundException;
import edu.aua.course_recommendation.model.AcademicStanding;
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

    @Transactional
    public void enroll(final UUID studentId, final String courseCode, String grade, String year, String semester) {
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
        enrollment.setYear(year);
        enrollment.setSemester(semester);
        enrollment.setUser(student);
        enrollment.setCourse(course);

        student.getEnrollments().add(enrollment);

        enrollmentRepository.save(enrollment);
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

        // Get the enrollment to remove from collection first
        Enrollment enrollment = enrollmentRepository.findByUserAndCourse(student, course);
        if (enrollment != null) {
            student.getEnrollments().remove(enrollment);
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

        User user = userRepository.findById(studentId).orElseThrow(
                () -> new UserNotFoundException("Student not found")
        );

        Course course = courseRepository.findByCode(courseCode)
                .orElseThrow(() -> new CourseNotFoundException("Course with code " + courseCode + " not found"));

        return new StudentAndCourse(user, course);
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

    public void calculateAcademicStanding(UUID studentId) {
        int totalCredits = getCompletedCourses(studentId).stream()
                .mapToInt(Course::getCredits)
                .sum();

        AcademicStanding standing = AcademicStanding.getStandingFromCredits(totalCredits);

        // Update the user's academic standing using the studentId parameter
        User student = userRepository.findById(studentId).orElseThrow();
        student.setAcademicStanding(standing);
        userRepository.save(student);
    }

    @Transactional
    public void enrollList(UUID studentId, List<EnrollmentRequestDto> requests) {
        User student = userRepository.findById(studentId).orElseThrow();

        for (EnrollmentRequestDto request : requests) {
            Course course = courseRepository.findByCode(request.courseCode())
                    .orElseThrow(() -> new CourseNotFoundException("Course with code " + request.courseCode() + " not found"));

            // Check if already enrolled
            if (enrollmentRepository.existsByUserAndCourse(student, course)) {
                continue; // Skip or throw exception if needed
            }

            // Create the enrollment
            EnrollmentId enrollmentId = new EnrollmentId();
            enrollmentId.setUserId(studentId);
            enrollmentId.setCourseId(course.getId());

            Enrollment enrollment = new Enrollment();
            enrollment.setId(enrollmentId);
            enrollment.setGrade(request.grade());
            enrollment.setYear(request.year());
            enrollment.setSemester(request.semester());
            enrollment.setUser(student);
            enrollment.setCourse(course);

            student.getEnrollments().add(enrollment);
        }

        // Save the student which will cascade to enrollments
        userRepository.save(student);
        calculateAcademicStanding(studentId);
    }

    @Transactional
    public void dropAll(UUID studentId) {
        User student = userRepository.findById(studentId).orElseThrow();
        List<Enrollment> enrollments = enrollmentRepository.findByUser_Id(studentId);

        // Clear the in-memory collection
        student.getEnrollments().clear();

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
