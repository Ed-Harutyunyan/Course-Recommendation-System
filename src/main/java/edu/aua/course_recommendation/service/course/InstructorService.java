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

        if (profileDto.name() == null || profileDto.name().isBlank()) {
            return null;
        }

        try {
            Instructor instructor = instructorRepository.findByName(profileDto.name())
                    .orElseGet(() -> Instructor.builder().name(profileDto.name()).build());

            String position = profileDto.position();
            if (position != null) {
                System.out.println("Position length before truncation: " + position.length());
            }

            // TODO: Update this to just normal validation
            instructor.setImageUrl(safelyTruncate(profileDto.image_url(), 490));
            instructor.setPosition(safelyTruncate(profileDto.position(), 490));
            instructor.setMobile(safelyTruncate(profileDto.mobile(), 45));
            instructor.setEmail(safelyTruncate(profileDto.email(), 245));
            instructor.setBio(safelyTruncate(profileDto.bio(), 65000));
            instructor.setOfficeLocation(safelyTruncate(profileDto.office_location(), 245));

            return instructorRepository.save(instructor);
        } catch (Exception e) {
            System.err.println("Error with instructor: " + profileDto.name() + " - " + e.getMessage());
            return null;
        }
    }

    private String safelyTruncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim()
                .replace('\u00A0', ' ')
                .replaceAll("[\\p{Cc}\\p{Cf}]", "");

        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    public Instructor createInstructor(InstructorProfileRequestDto profileDto) {
        if (profileDto.name() == null || profileDto.name().isBlank()) {
            return null;
        }

        if (instructorRepository.findByName(profileDto.name()).isPresent()) {
            throw new IllegalArgumentException("Instructor with name " + profileDto.name() + " already exists");
        }

        Instructor instructor = Instructor.builder()
                .name(profileDto.name())
                .imageUrl(safelyTruncate(profileDto.image_url(), 490))
                .position(safelyTruncate(profileDto.position(), 490))
                .mobile(safelyTruncate(profileDto.mobile(), 45))
                .email(safelyTruncate(profileDto.email(), 245))
                .bio(safelyTruncate(profileDto.bio(), 65000))
                .officeLocation(safelyTruncate(profileDto.office_location(), 245))
                .build();

        return instructorRepository.save(instructor);
    }

    public void deleteInstructor(UUID instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalArgumentException("Instructor not found with ID: " + instructorId));
        instructorRepository.delete(instructor);
    }
}
