package edu.aua.course_recommendation.service.schedule;

import edu.aua.course_recommendation.dto.request.MessageAndPossibleCourseDto;
import edu.aua.course_recommendation.dto.response.NeededCourseOfferingDto;
import edu.aua.course_recommendation.dto.response.RecommendationDto;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.entity.Schedule;
import edu.aua.course_recommendation.entity.ScheduleSlot;
import edu.aua.course_recommendation.exceptions.ScheduleNotFoundException;
import edu.aua.course_recommendation.exceptions.ScheduleValidationException;
import edu.aua.course_recommendation.exceptions.UserNotFoundException;
import edu.aua.course_recommendation.exceptions.ValidationError;
import edu.aua.course_recommendation.model.AcademicStanding;
import edu.aua.course_recommendation.model.DegreeScenarioType;
import edu.aua.course_recommendation.model.Requirement;
import edu.aua.course_recommendation.model.RequirementResult;
import edu.aua.course_recommendation.repository.CourseOfferingRepository;
import edu.aua.course_recommendation.repository.ScheduleRepository;
import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.audit.DegreeAuditServiceRouter;
import edu.aua.course_recommendation.service.auth.UserService;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final UserService userService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final DegreeAuditServiceRouter degreeAuditServiceRouter;

    public static final int MAX_CREDITS_PER_REGISTRATION = 15;
    private final CourseOfferingService courseOfferingService;
    private final PythonService pythonService;

    public Schedule getScheduleById(UUID id) {
        return scheduleRepository.findById(id).orElseThrow(
                () -> new ScheduleNotFoundException("Schedule with id " + id + " not found")
        );
    }

    public Schedule saveSchedule(Schedule schedule) {
        // Validate the schedule before saving
        validateSchedule(schedule);

        return scheduleRepository.save(schedule);
    }

    public void deleteSchedule(UUID id) {
        scheduleRepository.deleteById(id);
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public List<Schedule> getSchedulesByStudentId(UUID studentId) {
        return scheduleRepository.findByStudentId(studentId).orElseThrow(
                () -> new UserNotFoundException("Student with id" + studentId + " not found")
        );
    }

    public void validateSchedule(Schedule schedule) {
        if (schedule == null || schedule.getSlots() == null) {
            throw new ScheduleValidationException(
                    ValidationError.NULL_SCHEDULE,
                    "Schedule or slots cannot be null"
            );
        }

        // Calculate total credits
        int totalCredits = schedule.getSlots().stream()
                .mapToInt(ScheduleSlot::getCredits)
                .sum();

        if (totalCredits > MAX_CREDITS_PER_REGISTRATION) {
            throw new ScheduleValidationException(
                    ValidationError.EXCEEDS_CREDIT_LIMIT,
                    String.format("Total credits %d exceeds maximum allowed %d",
                            totalCredits, MAX_CREDITS_PER_REGISTRATION)
            );
        }

        // Check each slot
        for (ScheduleSlot slot : schedule.getSlots()) {
            Optional<CourseOffering> offeringOpt = courseOfferingRepository.findCourseOfferingsById(slot.getOfferingId());
            if (offeringOpt.isEmpty()) {
                throw new ScheduleValidationException(
                        ValidationError.INVALID_OFFERING,
                        String.format("Course offering with id %s not found", slot.getOfferingId())
                );
            }

            CourseOffering offering = offeringOpt.get();

            if (!prerequisitesMet(offering, schedule.getStudentId())) {
                throw new ScheduleValidationException(
                        ValidationError.PREREQUISITES_NOT_MET,
                        String.format("Prerequisites not met for course %s",
                                offering.getBaseCourse().getCode())
                );
            }

            if (!validAcademicStanding(offering, schedule.getStudentId())) {
                throw new ScheduleValidationException(
                        ValidationError.ACADEMIC_STANDING_INSUFFICIENT,
                        String.format("Academic standing insufficient for course %s",
                                offering.getBaseCourse().getCode())
                );
            }
        }

        if (hasTimeConflicts(schedule.getSlots())) {
            throw new ScheduleValidationException(
                    ValidationError.TIME_CONFLICT,
                    "Schedule contains time conflicts"
            );
        }
    }

    public boolean isValidSchedule(Schedule schedule) {
        try {
            validateSchedule(schedule);
            return true;
        } catch (ScheduleValidationException e) {
            return false;
        }
    }

    // =============== HELPER FUNCTIONS ===============
    public Optional<CourseOffering> findOffering(List<CourseOffering> available, String code, int currentCredits, List<ScheduleSlot> slots, UUID studentId) {
        return available.stream()
                .filter(off -> off.getBaseCourse().getCode().equals(code))
                .filter(off -> currentCredits + off.getBaseCourse().getCredits() <= MAX_CREDITS_PER_REGISTRATION)
                .filter(off -> !hasTimeConflict(off, slots))
                .filter(off -> prerequisitesMet(off, studentId))
                .filter(off -> validAcademicStanding(off, studentId))
                .min(Comparator.comparing(this::getEarliestTimeInMinutes));
    }

    private int getEarliestTimeInMinutes(CourseOffering offering) {
        if (offering.getTimes() == null || offering.getTimes().isEmpty() || "TBD".equalsIgnoreCase(offering.getTimes())) {
            return Integer.MAX_VALUE; // Place TBD times at the end
        }

        String[] slots = offering.getTimes().split(", ");
        int earliestTime = Integer.MAX_VALUE;

        for (String slot : slots) {
            String[] parts = slot.trim().split(" ");
            if (parts.length < 2) continue;

            // Calculate day value (MON=0, TUE=1, etc.)
            int dayValue;
            switch (parts[0].toUpperCase()) {
                case "MON":
                    dayValue = 0;
                    break;
                case "TUE":
                    dayValue = 1;
                    break;
                case "WED":
                    dayValue = 2;
                    break;
                case "THU":
                    dayValue = 3;
                    break;
                case "FRI":
                    dayValue = 4;
                    break;
                case "SAT":
                    dayValue = 5;
                    break;
                case "SUN":
                    dayValue = 6;
                    break;
                default:
                    dayValue = 7;
                    break;
            }

            // Parse the start time
            String[] timeRange = parts[1].split("-");
            if (timeRange.length < 2) continue;

            int startTimeMinutes = parseTimeToMinutes(timeRange[0]);
            int slotValue = dayValue * 24 * 60 + startTimeMinutes; // Day value + minutes

            earliestTime = Math.min(earliestTime, slotValue);
        }

        return earliestTime;
    }

    public List<NeededCourseOfferingDto> findValidOfferings(UUID studentId) {
        // Get the needed course offering DTOs
        List<NeededCourseOfferingDto> neededDtos = getNeededCourseOfferings(studentId);

        // Filter only those that the student can take based on prerequisites and academic standing
        return neededDtos.stream()
                .filter(dto -> validAcademicStanding(dto.getCourseOffering(), studentId))
                .filter(dto -> prerequisitesMet(dto.getCourseOffering(), studentId))
                .toList();
    }

    public List<NeededCourseOfferingDto> findValidOfferingsForPeriod(UUID studentId, String year, String semester) {
        // Get all valid offerings for the student
        List<NeededCourseOfferingDto> allValidDtos = findValidOfferings(studentId);

        // Filter for specific period only
        return allValidDtos.stream()
                .filter(dto -> {
                    CourseOffering offering = dto.getCourseOffering();
                    return offering.getYear().equals(year) && offering.getSemester().equals(semester);
                })
                .toList();
    }

    public List<NeededCourseOfferingDto> findValidOfferingsForPeriodWithMessage(UUID studentId, String year, String semester, String message) {
        // Get all valid offerings for the student
        List<NeededCourseOfferingDto> allValidDtos = findValidOfferingsForPeriod(studentId, year, semester);

        // Ask python for recommendations
        List<RecommendationDto> recommendationDtos = pythonService.sendMessageRecommendations(
                MessageAndPossibleCourseDto.builder()
                        .possibleCourseCodes(allValidDtos.stream()
                                .map(dto -> dto.getCourseOffering().getBaseCourse().getCode())
                                .toList())
                        .message(message)
                        .build()
        );

        // Create a map of course codes to their recommendation scores for efficient lookup
        Map<String, Double> courseScoreMap = recommendationDtos.stream()
                .collect(HashMap::new,
                        (map, dto) -> map.put(dto.courseCode(), Double.parseDouble(dto.score())),
                        HashMap::putAll);

        // Filter for specific period and recommendations, then sort by score (highest first)
        return allValidDtos.stream()
                .filter(dto -> courseScoreMap.containsKey(dto.getCourseOffering().getBaseCourse().getCode()))
                .sorted((dto1, dto2) -> Double.compare(
                        courseScoreMap.get(dto2.getCourseOffering().getBaseCourse().getCode()),
                        courseScoreMap.get(dto1.getCourseOffering().getBaseCourse().getCode())))
                .toList();
    }


    public List<NeededCourseOfferingDto> getNeededCourseOfferings(UUID studentId) {
        List<NeededCourseOfferingDto> result = new ArrayList<>();
        BaseDegreeAuditService baseDegreeAuditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        // 0. Peer Mentoring
        RequirementResult peerMentoring = baseDegreeAuditService.checkPeerMentoringRequirement(studentId);
        addOfferingsForRequirement(result, peerMentoring, courseOfferingService);

        // 1. First Aid & Civil Defense
        RequirementResult firstAid = baseDegreeAuditService.checkFirstAidAndCivilDefense(studentId);
        addOfferingsForRequirement(result, firstAid, courseOfferingService);

        // 2. Physical Education
        RequirementResult physEd = baseDegreeAuditService.checkPhysicalEducationRequirementsDetailed(studentId);
        addOfferingsForRequirement(result, physEd, courseOfferingService);

        // 3. Foundation Requirements
        RequirementResult foundation = baseDegreeAuditService.checkFoundationRequirementsDetailed(studentId);
        addOfferingsForRequirement(result, foundation, courseOfferingService);

        // 4. Core Courses
        RequirementResult core = baseDegreeAuditService.checkProgramCore(studentId);
        addOfferingsForRequirement(result, core, courseOfferingService);

        // 5. General Education
        RequirementResult genEd = baseDegreeAuditService.checkGeneralEducationRequirementsDetailed(studentId);
        addOfferingsForRequirement(result, genEd, courseOfferingService);

        // 6. Track Requirements
        DegreeScenarioType chosenTrack = baseDegreeAuditService.pickChosenTrack(studentId);
        List<String> trackCodes = baseDegreeAuditService.getTrackCourseCodes(chosenTrack);
        for (String code : trackCodes) {
            List<CourseOffering> offerings = courseOfferingService.getCourseOfferingsByCourseCodes(List.of(code));
            for (CourseOffering offering : offerings) {
                result.add(new NeededCourseOfferingDto(Requirement.TRACK, offering));
            }
        }

        // 7. Free Electives
        RequirementResult freeElective = baseDegreeAuditService.checkFreeElectiveRequirements(studentId);
        addOfferingsForRequirement(result, freeElective, courseOfferingService);

        // 8. Capstone
        RequirementResult capstone = baseDegreeAuditService.checkCapstoneRequirement(studentId);
        addOfferingsForRequirement(result, capstone, courseOfferingService);

        return result;
    }

    private void addOfferingsForRequirement(List<NeededCourseOfferingDto> result, RequirementResult reqResult, CourseOfferingService courseOfferingService) {
        List<CourseOffering> offerings = courseOfferingService.getCourseOfferingsByCourseCodes(reqResult.getPossibleCourseCodes());
        for (CourseOffering offering : offerings) {
            // Check if this offering already exists in the result list
            boolean offeringExists = result.stream()
                    .anyMatch(dto -> dto.getCourseOffering().getId().equals(offering.getId()));

            if (!offeringExists) {
                result.add(new NeededCourseOfferingDto(reqResult.getRequirementName(), offering));
            }
        }
    }

    // Checks if the student can take UPPER course.
    // Only Sophomores and above can take upper courses
    private boolean validAcademicStanding(CourseOffering off, UUID studentId) {
        AcademicStanding standing = userService.getAcademicStanding(studentId);

        if (courseService.isUpperDivision(off.getBaseCourse().getCode())) {
            // Upper division courses require the student to be at least sophomore
            return standing == AcademicStanding.SOPHOMORE
                    || standing == AcademicStanding.JUNIOR
                    || standing == AcademicStanding.SENIOR;
        } else {
            // For lower division courses, assume all students (including freshmen) are eligible
            return true;
        }
    }


    public boolean prerequisitesMet(CourseOffering offering, UUID studentId) {
        // Get prerequisites from the base course
        Set<String> prerequisites = offering.getBaseCourse().getPrerequisites();

        // If no prerequisites, always return true
        if (prerequisites == null || prerequisites.isEmpty()) {
            return true;
        }

        // Get the student's completed course codes
        Set<String> completedCourses = new HashSet<>(enrollmentService.getCompletedCourseCodes(studentId));
        Set<String> effectivePrerequisites = new HashSet<>();

        // TODO: This is so so bad, why would anyone code like this?
        // TODO: Somehow figure out how to handle prerequisites that are not exact course codes...
        for (String prereq : prerequisites) {
            if (prereq.contains("EQCALC1")) {
                effectivePrerequisites.add("CS100");
            } else if (prereq.contains("EQCALC2")) {
                effectivePrerequisites.add("CS101");
            } else if (prereq.contains("EQDATASTRC")) {
                effectivePrerequisites.add("CS121");
            } else if (prereq.contains("EQOOP")) {
                effectivePrerequisites.add("CS110");
            } else {
                effectivePrerequisites.add(prereq);
            }
        }

        // Check if all effective prerequisites are in the completed courses
        return completedCourses.containsAll(effectivePrerequisites);
    }


    // Naive approach
    // Does String Parsing
    // Works with Jenzabar's Time Format
    private boolean hasTimeConflict(CourseOffering newOffering, List<ScheduleSlot> existingSlots) {
        if (newOffering.getTimes() == null || newOffering.getTimes().isEmpty()) {
            return false;
        }

        return existingSlots.stream()
                .filter(slot -> slot.getOfferingId() != null)
                .map(slot -> courseOfferingRepository.findCourseOfferingsById(slot.getOfferingId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(existing -> doTimesConflict(existing.getTimes(), newOffering.getTimes()));
    }

    private boolean doTimesConflict(String times1, String times2) {

        // Handle TBD case
        if ("TBD".equalsIgnoreCase(times1) || "TBD".equalsIgnoreCase(times2)) {
            return false;
        }

        // Handle null or empty cases
        if (times1 == null || times2 == null || times1.isEmpty() || times2.isEmpty()) {
            return false;
        }

        // Split multiple time slots
        String[] slots1 = times1.split(", ");
        String[] slots2 = times2.split(", ");

        // Check each combination of time slots
        for (String slot1 : slots1) {
            for (String slot2 : slots2) {
                if (doSingleSlotsConflict(slot1, slot2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doSingleSlotsConflict(String slot1, String slot2) {
        String[] parts1 = slot1.trim().split(" ");
        String[] parts2 = slot2.trim().split(" ");

        if (parts1.length < 2 || parts2.length < 2) {
            return false;
        }

        // Check if days overlap
        if (!parts1[0].equals(parts2[0])) {
            return false;
        }

        // Parse times
        String[] timeRange1 = parts1[1].split("-");
        String[] timeRange2 = parts2[1].split("-");

        if (timeRange1.length != 2 || timeRange2.length != 2) {
            return false;
        }

        int start1 = parseTimeToMinutes(timeRange1[0]);
        int end1 = parseTimeToMinutes(timeRange1[1]);
        int start2 = parseTimeToMinutes(timeRange2[0]);
        int end2 = parseTimeToMinutes(timeRange2[1]);

        return !(end1 <= start2 || end2 <= start1);
    }

    private int parseTimeToMinutes(String time) {
        // Remove am/pm and convert to 24-hour format
        time = time.toLowerCase();
        boolean isPM = time.endsWith("pm");
        time = time.replace("am", "").replace("pm", "");

        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);

        if (isPM && hours != 12) {
            hours += 12;
        }
        if (!isPM && hours == 12) {
            hours = 0;
        }

        return hours * 60 + minutes;
    }

    private boolean hasTimeConflicts(List<ScheduleSlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                Optional<CourseOffering> offering1 = courseOfferingRepository.findCourseOfferingsById(slots.get(i).getOfferingId());
                Optional<CourseOffering> offering2 = courseOfferingRepository.findCourseOfferingsById(slots.get(j).getOfferingId());

                if (offering1.isPresent() && offering2.isPresent() &&
                        doTimesConflict(offering1.get().getTimes(), offering2.get().getTimes())) {
                    return true;
                }
            }
        }
        return false;
    }
}
