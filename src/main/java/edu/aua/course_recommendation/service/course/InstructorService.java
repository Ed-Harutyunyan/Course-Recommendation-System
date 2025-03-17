package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.entity.Instructor;
import edu.aua.course_recommendation.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstructorService {

    public final InstructorRepository instructorRepository;

    public Instructor getOrCreateInstructor(String name) {
        return instructorRepository.findByName(name)
                .orElseGet(() -> instructorRepository.save(Instructor.builder().name(name).build()));
    }

    public List<Instructor> getAllInstructors() {
        return instructorRepository.findAll();
    }

    public Instructor getInstructorById(UUID id) {
        return instructorRepository.findById(id).orElse(null);
    }

    public Instructor getInstructorByName(String name) {
        return instructorRepository.findByName(name).orElse(null);
    }
}
