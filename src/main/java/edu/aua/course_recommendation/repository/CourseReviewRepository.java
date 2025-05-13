package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {
    List<CourseReview> findByCourseId(UUID courseId);
    List<CourseReview> findByUserId(UUID userId);
}