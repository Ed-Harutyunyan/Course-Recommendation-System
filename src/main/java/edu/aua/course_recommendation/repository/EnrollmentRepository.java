package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {
    boolean existsByUserAndCourse(User student, Course course);

    void deleteByUserAndCourse(User student, Course course);

    List<Enrollment> findByUser_Id(UUID studentId);
}
