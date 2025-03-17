package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {
    boolean existsByUserAndCourseOffering(User student, CourseOffering courseOffering);

    void deleteByUserAndCourseOffering(User student, CourseOffering courseOffering);

    List<Enrollment> findByUser_Id(UUID studentId);
}
