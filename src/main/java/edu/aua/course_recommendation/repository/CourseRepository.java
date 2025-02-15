package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface  CourseRepository extends JpaRepository<Course, UUID> {
    Optional<Course> findCourseById(UUID courseId);
}
