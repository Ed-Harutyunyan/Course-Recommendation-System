package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.dto.response.CourseOfferingDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.entity.Instructor;
import edu.aua.course_recommendation.exceptions.CourseOfferingAlreadyExistsException;
import edu.aua.course_recommendation.exceptions.CourseOfferingNotFoundException;
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
                        .findByBaseCourse_CodeAndYearAndSemesterAndInstructor_NameAndSection(
                                courseOfferingDto.courseCode(),
                                courseOfferingDto.year(),
                                courseOfferingDto.semester(),
                                courseOfferingDto.instructor(),
                                courseOfferingDto.section());

        if (existingOffering.isPresent()) {
            log.error("Course Offering already exists for this course, instructor, and section in the given year and semester");
            throw new CourseOfferingAlreadyExistsException(
                    courseOfferingDto.courseCode(),
                    courseOfferingDto.year(),
                    courseOfferingDto.semester());
        }

        CourseOffering courseOffering = createCourseOfferingWithBaseCourseAndInstructor(courseOfferingDto);
        return courseOfferingRepository.save(courseOffering);
    }

    @Transactional()
    public List<CourseOffering> createCourseOfferings(List<CourseOfferingDto> courseOfferingDtos) {
        List<CourseOffering> createdOfferings = new ArrayList<>();
        for (CourseOfferingDto dto : courseOfferingDtos) {
            Optional<CourseOffering> existingOffering =
                    courseOfferingRepository.findByBaseCourse_CodeAndYearAndSemesterAndInstructor_NameAndSection(
                            dto.courseCode(), dto.year(), dto.semester(), dto.instructor(), dto.section());

            if (existingOffering.isPresent()) {
                log.info("Skipping existing offering: code={}, year={}, semester={}, instructor={}, section={}",
                        dto.courseCode(), dto.year(), dto.semester(), dto.instructor(), dto.section());
                continue; // Skip if already exists
            }

            CourseOffering courseOffering = createCourseOfferingWithBaseCourseAndInstructor(dto);
            createdOfferings.add(courseOffering);
        }

        return courseOfferingRepository.saveAll(createdOfferings);
    }

    private CourseOffering createCourseOfferingWithBaseCourseAndInstructor(CourseOfferingDto dto) {

        Course baseCourse = courseService.getOrCreateCourse(courseMapper.toCourseDto(dto));
        Instructor instructor = instructorService.getOrCreateInstructor(dto.instructor());

        return CourseOffering.builder()
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
    }


    @Transactional(readOnly = true)
    public List<CourseOffering> getAllCourseOfferings() {
        return courseOfferingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Course getBaseCourseByOfferingId(UUID offeringId) {
        CourseOffering courseOffering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new CourseOfferingNotFoundException("Course offering not found with id: " + offeringId));
        return courseOffering.getBaseCourse();
    }

    @Transactional(readOnly = true)
    public CourseOffering getCourseOfferingById(UUID id) {
        return courseOfferingRepository.findById(id).orElseThrow(
                () -> new CourseOfferingNotFoundException("Course offering not found with id: " + id)
        );
    }

    @Transactional(readOnly = true)
    public List<CourseOffering> getCourseOfferingsByCourseCodes(List<String> courseCodes) {

        if (courseCodes == null || courseCodes.isEmpty()) {
            return new ArrayList<>();
        }

        return courseOfferingRepository.findByBaseCourse_CodeIn(courseCodes);
    }

    @Transactional
    public List<CourseOffering> getCourseOfferingsByYearAndSemester(String year, String semester) {
        return courseOfferingRepository.findByYearAndSemester(year, semester);
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

    @Transactional(readOnly = true)
    public Optional<CourseOffering> findOfferingByBaseCourseCode(String courseCode) {
        return courseOfferingRepository.findFirstByBaseCourse_Code(courseCode);
    }

    @Transactional(readOnly = true)
    public List<CourseOffering> getCourseOfferingsByCourseCode(String code) {
        return courseOfferingRepository.findAllByBaseCourse_Code(code);
    }

    /**
     * Retrieves a list of course offerings for a specific year, semester, and course code.
     *
     * @param year     the academic year of the course offerings (e.g., "2023").
     * @param semester the semester of the course offerings (e.g., "Fall").
     * @param code     the code of the base course (e.g., "CS101").
     * @return a list of course offerings matching the specified criteria. If no offerings are found,
     *         an empty list is returned, and a warning is logged.
     */
    @Transactional(readOnly = true)
    public List<CourseOffering> getCourseOfferingsByYearSemesterAndCode(String year, String semester, String code) {
        List<CourseOffering> offerings = courseOfferingRepository.findByYearAndSemesterAndBaseCourse_Code(year, semester, code);
        if (offerings.isEmpty()) {
            log.warn("No course offerings found for year: {}, semester: {}, and code: {}", year, semester, code);
        }
        return offerings;
    }

    public List<CourseOffering> getAllCourseOfferingsByYearAndSemester(String year, String semester) {
        List<CourseOffering> offerings = courseOfferingRepository.findByYearAndSemester(year, semester);
        if (offerings.isEmpty()) {
            log.warn("No course offerings found for year: {} and semester: {}", year, semester);
        }
        return offerings;
    }
}
