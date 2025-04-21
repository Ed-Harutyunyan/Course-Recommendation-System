package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface  CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByCode(String code);

    @Query("SELECT c FROM Course c JOIN c.themes t WHERE t IN (?1)")
    List<Course> findByThemes(List<Integer> themes);

    @Query("SELECT c FROM Course c WHERE ?1 MEMBER OF c.themes")
    List<Course> findByTheme(Integer theme);

    boolean existsByCode(String code);

    void deleteByCode(String code);

}
