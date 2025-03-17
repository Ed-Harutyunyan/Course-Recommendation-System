package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {
    boolean existsByUserAndCourseOffering(User student, CourseOffering courseOffering);

    void deleteByUserAndCourseOffering(User student, CourseOffering courseOffering);
}
