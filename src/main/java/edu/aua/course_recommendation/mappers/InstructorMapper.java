package edu.aua.course_recommendation.mappers;

import edu.aua.course_recommendation.dto.response.InstructorResponseDto;
import edu.aua.course_recommendation.entity.Instructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InstructorMapper {

    public InstructorResponseDto toResponseDto(Instructor instructor) {
        if (instructor == null) {
            return null;
        }

        return new InstructorResponseDto(
                instructor.getId(),
                instructor.getName(),
                instructor.getImageUrl(),
                instructor.getPosition(),
                instructor.getMobile(),
                instructor.getEmail(),
                instructor.getBio(),
                instructor.getOfficeLocation()
        );
    }

    public List<InstructorResponseDto> toResponseDtoList(List<Instructor> instructors) {
        if (instructors == null) {
            return List.of();
        }

        return instructors.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }
}