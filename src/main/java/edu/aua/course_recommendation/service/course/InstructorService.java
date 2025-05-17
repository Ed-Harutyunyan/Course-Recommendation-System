package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.dto.request.InstructorProfileRequestDto;
import edu.aua.course_recommendation.entity.Instructor;
import edu.aua.course_recommendation.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Instructor updateInstructorProfile(InstructorProfileRequestDto profileDto) {
        Instructor instructor = instructorRepository.findByName(profileDto.name())
                .orElseGet(() -> Instructor.builder().name(profileDto.name()).build());

        instructor.setImageUrl(profileDto.image_url());
        instructor.setPosition(profileDto.position());
        instructor.setMobile(profileDto.mobile());
        instructor.setEmail(profileDto.email());
        instructor.setBio(profileDto.bio());
        instructor.setOfficeLocation(profileDto.office_location());

        return instructorRepository.save(instructor);
    }

}
