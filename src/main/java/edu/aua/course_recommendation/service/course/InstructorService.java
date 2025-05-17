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
        // Skip if name is null or blank
        if (profileDto.name() == null || profileDto.name().isBlank()) {
            return null;
        }

        Instructor instructor = instructorRepository.findByName(profileDto.name())
                .orElseGet(() -> Instructor.builder().name(profileDto.name()).build());

        // Apply values with constraints
        instructor.setImageUrl(truncateIfNeeded(profileDto.image_url(), 500));
        instructor.setPosition(truncateIfNeeded(profileDto.position(), 500));
        instructor.setMobile(truncateIfNeeded(profileDto.mobile(), 50));
        instructor.setEmail(truncateIfNeeded(profileDto.email(), 255));
        instructor.setBio(truncateIfNeeded(profileDto.bio(), 65535));
        instructor.setOfficeLocation(truncateIfNeeded(profileDto.office_location(), 255));

        return instructorRepository.save(instructor);
    }

    private String truncateIfNeeded(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

}
