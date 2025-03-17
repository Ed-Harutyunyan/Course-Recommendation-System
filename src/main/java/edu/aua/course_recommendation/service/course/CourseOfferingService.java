package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.dto.CourseOfferingDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.entity.Instructor;
import edu.aua.course_recommendation.exceptions.CourseOfferingAlreadyExistsException;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.repository.CourseOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseOfferingService {

    private static final Logger log = LoggerFactory.getLogger(CourseOfferingService.class);

    private final CourseService courseService;
    private final InstructorService instructorService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseMapper courseMapper;

    @Transactional
    public CourseOffering createCourseOffering(CourseOfferingDto courseOfferingDto) {
        Optional<CourseOffering> existingOffering =
                courseOfferingRepository
                        .findByBaseCourse_CodeAndYearAndSemester(
                                courseOfferingDto.courseCode(),
                                courseOfferingDto.year(),
                                courseOfferingDto.semester());

        // This means that the course offering already exists
        if (existingOffering.isPresent()) {
            log.error("Course Offering already exists for this course in the given year and semester");
            throw new CourseOfferingAlreadyExistsException(
                    courseOfferingDto.courseCode(),
                    courseOfferingDto.year(),
                    courseOfferingDto.semester());
        }

        System.out.println("DTO: " + courseOfferingDto);

        Course baseCourse = courseService.getOrCreateCourse(courseMapper.toCourseDto(courseOfferingDto));
        System.out.println("baseCourse: " + baseCourse);

        Instructor instructor = instructorService.getOrCreateInstructor(courseOfferingDto.instructor());
        System.out.println("instructor: " + instructor);

        // Some of the fields that are saved here really serve 0 purpose and are probably best removed
        // e.g takenSeats, spacesWaiting
        CourseOffering courseOffering = CourseOffering.builder()
                .baseCourse(baseCourse)
                .section(courseOfferingDto.section())
                .session(courseOfferingDto.session())
                .campus(courseOfferingDto.campus())
                .instructor(instructor)
                .times(courseOfferingDto.times())
                .takenSeats(courseOfferingDto.takenSeats())
                .spacesWaiting(courseOfferingDto.spacesWaiting())
                .deliveryMethod(courseOfferingDto.deliveryMethod())
                .distLearning(courseOfferingDto.distLearning())
                .location(courseOfferingDto.location())
                .year(courseOfferingDto.year())
                .semester(courseOfferingDto.semester())
                .build();

        return courseOfferingRepository.save(courseOffering);
    }

    @Transactional(readOnly = true)
    public List<CourseOffering> createCourseOfferings(List<CourseOfferingDto> courseOfferingDtos) {
        List<CourseOffering> createdOfferings = new ArrayList<>();

        for (CourseOfferingDto dto : courseOfferingDtos) {
            Optional<CourseOffering> existingOffering =
                    courseOfferingRepository.findByBaseCourse_CodeAndYearAndSemester(
                            dto.courseCode(), dto.year(), dto.semester());

            if (existingOffering.isPresent()) {
                log.info("Skipping existing offering: code={}, year={}, semester={}", dto.courseCode(), dto.year(), dto.semester());
                continue; // Skip if already exists
            }

            Course baseCourse = courseService.getOrCreateCourse(courseMapper.toCourseDto(dto));
            Instructor instructor = instructorService.getOrCreateInstructor(dto.instructor());

            CourseOffering courseOffering = CourseOffering.builder()
                    .baseCourse(baseCourse)
                    .section(dto.section())
                    .session(dto.session())
                    .campus(dto.campus())
                    .instructor(instructor)
                    .times(dto.times())
                    .takenSeats(dto.takenSeats())
                    .spacesWaiting(dto.spacesWaiting())
                    .deliveryMethod(dto.deliveryMethod())
                    .distLearning(dto.distLearning())
                    .location(dto.location())
                    .year(dto.year())
                    .semester(dto.semester())
                    .build();

            createdOfferings.add(courseOffering);
        }

        return courseOfferingRepository.saveAll(createdOfferings); // Saves all at once
    }


    @Transactional(readOnly = true)
    public List<CourseOffering> getAllCourseOfferings() {
        return courseOfferingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CourseOffering getCourseOfferingById(UUID id) {
        return courseOfferingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course offering not found with id: " + id));
    }

    @Transactional
    public void deleteCourseOfferingById(UUID id) {
        CourseOffering offering = courseOfferingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course offering not found with id: " + id));

        // 1. Remove from the parent's collections
        Course baseCourse = offering.getBaseCourse();
        if (baseCourse != null) {
            baseCourse.getOfferings().remove(offering);
        }

        Instructor instructor = offering.getInstructor();
        if (instructor != null) {
            instructor.getCourseOfferings().remove(offering);
        }

        courseOfferingRepository.delete(offering);
    }

    @Transactional
    public void deleteAllCourseOfferings() {
        // 1. Fetch all existing offerings
        List<CourseOffering> allOfferings = courseOfferingRepository.findAll();

        // 2. Remove each offering from its parent Course and Instructor
        for (CourseOffering offering : allOfferings) {
            // Remove from the Course's list
            if (offering.getBaseCourse() != null) {
                offering.getBaseCourse().getOfferings().remove(offering);
            }

            // Remove from the Instructor's list
            if (offering.getInstructor() != null) {
                offering.getInstructor().getCourseOfferings().remove(offering);
            }
        }

        // 3. Delete them all at once
        courseOfferingRepository.deleteAll(allOfferings);
    }

}
