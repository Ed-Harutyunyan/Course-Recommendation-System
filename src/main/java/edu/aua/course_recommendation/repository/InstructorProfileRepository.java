package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.InstructorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, UUID> {

}
