package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, UUID> {

    Optional<CourseOffering> findByBaseCourse_CodeAndYearAndSemester(String s, String year, String semester);

    Optional<CourseOffering> findCourseOfferingsById(UUID id);

    List<CourseOffering> findByYearAndSemester(String year, String semester);
}
