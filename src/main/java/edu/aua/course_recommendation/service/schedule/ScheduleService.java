package edu.aua.course_recommendation.service.schedule;

import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.entity.Schedule;
import edu.aua.course_recommendation.entity.ScheduleSlot;
import edu.aua.course_recommendation.exceptions.ScheduleNotFoundException;
import edu.aua.course_recommendation.exceptions.ScheduleValidationException;
import edu.aua.course_recommendation.exceptions.UserNotFoundException;
import edu.aua.course_recommendation.exceptions.ValidationError;
import edu.aua.course_recommendation.model.AcademicStanding;
import edu.aua.course_recommendation.model.DegreeScenarioType;
import edu.aua.course_recommendation.repository.CourseOfferingRepository;
import edu.aua.course_recommendation.repository.ScheduleRepository;
import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.auth.UserService;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final UserService userService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final BaseDegreeAuditService baseDegreeAuditService;

    public static final int MAX_CREDITS_PER_REGISTRATION = 15;
    private final CourseOfferingService courseOfferingService;

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
                .findFirst();
    }

    public List<CourseOffering> findValidOfferings(UUID studentId) {
        // Get the course offerings that the student needs to take
        List<CourseOffering> neededOfferings = getNeededCourseOfferings(studentId);

        // Filter only those that the student can take based on prerequisites and academic standing
        return neededOfferings.stream()
                .filter(off -> validAcademicStanding(off, studentId))
                .filter(off -> prerequisitesMet(off, studentId))
                .toList();
    }

    public List<CourseOffering> getNeededCourseOfferings(UUID studentId) {
        // Map to keep track of which category a course code belongs to
        Map<String, Integer> categoryPriority = new LinkedHashMap<>();
        int priorityIndex = 0;

        // 0. Peer Mentoring
        List<String> peerMentoring = baseDegreeAuditService.checkPeerMentoringRequirement(studentId).getPossibleCourseCodes();
        for (String code : peerMentoring) {
            categoryPriority.put(code, priorityIndex);
        }
        priorityIndex++;

        // 1. First Aid & Civil Defense
        List<String> firstAidCodes = baseDegreeAuditService.checkFirstAidAndCivilDefense(studentId).getPossibleCourseCodes();
        for (String code : firstAidCodes) {
            categoryPriority.put(code, priorityIndex);
        }
        priorityIndex++;

        // 2. Physical Education
        List<String> physEdCodes = baseDegreeAuditService.checkPhysicalEducationRequirementsDetailed(studentId).getPossibleCourseCodes();
        for (String code : physEdCodes) {
            categoryPriority.put(code, priorityIndex);
        }
        priorityIndex++;

        // 3. Foundation Requirements
        List<String> foundationCodes = baseDegreeAuditService.checkFoundationRequirementsDetailed(studentId).getPossibleCourseCodes();
        for (String code : foundationCodes) {
            categoryPriority.put(code, priorityIndex);
        }
        priorityIndex++;

        // 4. Core Courses
        List<String> coreCodes = baseDegreeAuditService.checkProgramCore(studentId).getPossibleCourseCodes();
        for (String code : coreCodes) {
            categoryPriority.put(code, priorityIndex);
        }
        priorityIndex++;

        // 5. General Education
        List<String> genEdCodes = baseDegreeAuditService.checkGeneralEducationRequirementsDetailed(studentId).getPossibleCourseCodes();
        for (String code : genEdCodes) {
            categoryPriority.put(code, priorityIndex);
        }
        priorityIndex++;

        // 6. Track Requirements
        DegreeScenarioType chosenTrack = baseDegreeAuditService.pickChosenTrack(studentId);
        List<String> trackCodes = baseDegreeAuditService.getTrackCourseCodes(chosenTrack);
        for (String code : trackCodes) {
            categoryPriority.put(code, priorityIndex);
        }
        priorityIndex++;

        // 7. Free Electives
        List<String> freeElectiveCodes = baseDegreeAuditService.checkFreeElectiveRequirements(studentId).getPossibleCourseCodes();
        for (String code : freeElectiveCodes) {
            categoryPriority.put(code, priorityIndex);
        }
        priorityIndex++;

        // 8. Capstone
        List<String> capstoneCodes = baseDegreeAuditService.checkCapstoneRequirement(studentId).getPossibleCourseCodes();
        for (String code : capstoneCodes) {
            categoryPriority.put(code, priorityIndex);
        }

        // Combine all codes while maintaining uniqueness
        List<String> allCodes = new ArrayList<>(categoryPriority.keySet());

        // Get course offerings for all needed codes
        List<CourseOffering> offerings = courseOfferingService.getCourseOfferingsByCourseCodes(allCodes);

        // Sort offerings by category priority
        offerings.sort((a, b) -> {
            Integer aPriority = categoryPriority.getOrDefault(a.getBaseCourse().getCode(), Integer.MAX_VALUE);
            Integer bPriority = categoryPriority.getOrDefault(b.getBaseCourse().getCode(), Integer.MAX_VALUE);
            return aPriority.compareTo(bPriority);
        });

        return offerings;
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

        // Check if all prerequisites are in the completed courses
        return completedCourses.containsAll(prerequisites);
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
