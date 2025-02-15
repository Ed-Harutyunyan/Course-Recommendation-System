package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.Enrollment;
import edu.aua.course_recommendation.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    boolean existsByStudentAndCourse(Student student, Course course);

    void deleteByStudentAndCourse(Student student, Course course);
}
