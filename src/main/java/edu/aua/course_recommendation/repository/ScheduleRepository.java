package edu.aua.course_recommendation.repository;

import edu.aua.course_recommendation.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    Optional<List<Schedule>> findByStudentId(UUID studentId);
}
