package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.Enrollment;
import edu.aua.course_recommendation.entity.EnrollmentId;
import edu.aua.course_recommendation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {
    boolean existsByUserAndCourse(User student, Course course);

    void deleteByUserAndCourse(User student, Course course);
}
