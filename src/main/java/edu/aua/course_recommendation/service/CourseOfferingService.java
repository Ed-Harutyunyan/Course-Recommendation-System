package edu.aua.course_recommendation.service;

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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseOfferingService {

    private static final Logger log = LoggerFactory.getLogger(CourseOfferingService.class);

    private final CourseService courseService;
    private final InstructorService instructorService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseMapper courseMapper;

    public CourseOffering createCourseOffering(CourseOfferingDto courseOfferingDto) {
        Optional<CourseOffering> existingOffering =
                courseOfferingRepository
                        .findByBaseCourse_CodeAndYearAndSemester(
                                courseOfferingDto.courseCode(),
                                courseOfferingDto.year(),
                                courseOfferingDto.semester());

        // This means that the course offering already exists
        if (existingOffering.isPresent()) {
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

}
