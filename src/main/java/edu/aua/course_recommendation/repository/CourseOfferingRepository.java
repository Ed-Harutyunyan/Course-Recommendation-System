package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, UUID> {

    Optional<CourseOffering> findByBaseCourse_CodeAndYearAndSemester(String s, String year, String semester);

    Optional<CourseOffering> findCourseOfferingsById(UUID id);

    Optional<CourseOffering> findFirstByBaseCourse_Code(String code);

    List<CourseOffering> findAllByBaseCourse_Code(String code);

    List<CourseOffering> findByYearAndSemester(String year, String semester);

    List<CourseOffering> findByBaseCourse_CodeIn(List<String> courseCodes);
}
