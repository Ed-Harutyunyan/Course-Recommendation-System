package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.Instructor;
import edu.aua.course_recommendation.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstructorService {

    public final InstructorRepository instructorRepository;

    public Instructor getOrCreateInstructor(String name) {
        return instructorRepository.findByName(name)
                .orElseGet(() -> instructorRepository.save(Instructor.builder().name(name).build()));
    }
}
