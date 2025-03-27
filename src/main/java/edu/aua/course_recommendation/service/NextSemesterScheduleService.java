package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.model.*;
import edu.aua.course_recommendation.repository.CourseOfferingRepository;
import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.audit.GenedClusteringService;
import edu.aua.course_recommendation.service.auth.UserService;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class NextSemesterScheduleService {

    private static final int MAX_CREDITS_PER_REGISTRATION = 15;

    private final EnrollmentService enrollmentService;
    private final BaseDegreeAuditService baseDegreeAuditService;
    private final CourseOfferingService courseOfferingService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final GenedClusteringService genedClusteringService;
    private final CourseService courseService;
    private final UserService userService;
    // Python will go here to fetch the recs

    // TODO: Change so year and semester in inferred from local time and no need to provide it
    public NextSemesterSchedule getNextSemester(UUID studentId, String year, String semester) {

        // 1. Fetch the next semesters offerings
        List<CourseOffering> nextSemesterOfferings = courseOfferingService.
                getCourseOfferingsByYearAndSemester(year, semester);


        // 2. Filter out courses the student has already completed or is currently enrolled in
        Set<String> completedCodes = new HashSet<>(enrollmentService.getCompletedCourseCodes(studentId));
        List<CourseOffering> available = nextSemesterOfferings.stream()
                .filter(off -> !completedCodes.contains(off.getBaseCourse().getCode()))
                .toList();

        // 3. Initialize the schedule
        List<ScheduleSlot> slots = new ArrayList<>();
        int currentCredits = 0;

        // 4. Add zero-credit items (First Aid, Civil Defense, 1 PhysEd if needed)
        // TODO: We'll add a waiver check that sets completed to 4 if the student has a waiver for physed in audit service
        currentCredits += addZeroCreditItems(slots, studentId, available, currentCredits);

        // 5. Add 1 Foundation if needed
        // This will be IN ORDER (They are all prerequisites to each other)
        currentCredits += addFoundationIfNeeded(slots, studentId, available, currentCredits);

        // 6. Add up to 3 core if GenEd is not done (or 4 if done)
        currentCredits += addCoreCoursesIfNeeded(slots, studentId, available, currentCredits);

        // 7. If GenEd not done, add 1 GenEd
        currentCredits += addGenEdIfNeeded(slots, studentId, available, currentCredits);

        // 8. Add 1 Track if there's space
        // This will only happen if the student has CORE and GENED completed
        currentCredits += addTrackIfNeeded(slots, studentId, available, currentCredits);

        // 9. Add 1 Free Elective if there's still space
        currentCredits += addFreeElectiveIfNeeded(slots, studentId, available, currentCredits);

        // 10. Capstone only if all else is complete
        currentCredits += addCapstoneIfPossible(slots, studentId, available, currentCredits);

        // Return the final schedule
        return new NextSemesterSchedule(slots);
    }


    // =============== 4. ZERO-CREDIT ITEMS ===============
    // =============== FIRST AID, CIVIL DEFENSE AND PHYSED ===============
    private int addZeroCreditItems(List<ScheduleSlot> slots, UUID studentId,
                                   List<CourseOffering> available,
                                   int currentCredits) {
        // Check First Aid and Civil Defense
        RequirementResult facd = baseDegreeAuditService.checkFirstAidAndCivilDefense(studentId);

        if (!facd.isSatisfied()) {
            for (String missingCode : facd.getPossibleCourseCodes()) {
                Optional<CourseOffering> offering = courseOfferingService.findOfferingByBaseCourseCode(missingCode);

                // Only add slot if offering exists
                offering.ifPresent(courseOffering -> slots.add(new ScheduleSlot(
                        CourseType.FIRST_AID_CD,
                        courseOffering.getId(),
                        0,
                        "N/A"
                )));
            }
        }

        // PhysEd check
        RequirementResult phed = baseDegreeAuditService.checkPhysicalEducationRequirementsDetailed(studentId);

        if (!phed.isSatisfied()) {
            // Try to find an offering for the first missing PE code
            String firstMissingPE = phed.getPossibleCourseCodes().stream()
                    .findFirst()
                    .orElse(null);

            if (firstMissingPE != null) {
                Optional<CourseOffering> peOffering = findOffering(available, firstMissingPE, currentCredits, slots, studentId);
                peOffering.ifPresent(courseOffering -> slots.add(new ScheduleSlot(
                        CourseType.PE,
                        courseOffering.getId(),
                        0,
                        courseOffering.getTimes()
                )));
            }
        }

        return 0;

    }

    // =============== 5. FOUNDATION ===============
    private int addFoundationIfNeeded(
            List<ScheduleSlot> slots,
            UUID studentId,
            List<CourseOffering> available,
            int currentCredits
    ) {
        RequirementResult foundation = baseDegreeAuditService.checkFoundationRequirementsDetailed(studentId);

        // All foundation courses are complete
        if (foundation.isSatisfied()) {
            return 0;
        }

        // Pick 1 missing foundation code
        // Since all foundations are in order it makes sense to call findFirst() here
        String firstMissing = foundation.getPossibleCourseCodes().stream()
                .findFirst().orElse(null);

        System.out.println("First missing foundation code: " + firstMissing);

        if (firstMissing == null) {
            return 0;
        }

        // find an actual offering from 'available'
        Optional<CourseOffering> off = findOffering(available, firstMissing, currentCredits, slots, studentId);
        if (off.isPresent()) {
            // Add the slot if an offering is found
            slots.add(new ScheduleSlot(
                    CourseType.FOUNDATION,
                    off.get().getId(),
                    3,
                    off.get().getTimes()
            ));
            return 3;
        }

        return 0;


    }

    // =============== 6. CORE COURSES ===============
    private int addCoreCoursesIfNeeded(
            List<ScheduleSlot> slots,
            UUID studentId,
            List<CourseOffering> available,
            int currentCredits
    ) {
        // 1. Check if the student still needs any core courses
        RequirementResult coreResult = baseDegreeAuditService.checkProgramCore(studentId);
        if (coreResult.isSatisfied()) {
            // Core is already complete
            return 0;
        }

        // 3. Gather the missing core codes
        List<String> missingCoreCodes = new ArrayList<>(coreResult.getPossibleCourseCodes());
        System.out.println("Missing core codes: " + missingCoreCodes);

        int added = 0;
        // 4. While we have room for more core (and haven't hit the credit limit)
        while (added < 3 && currentCredits < MAX_CREDITS_PER_REGISTRATION) { // TODO: Hardcoded value
            if (missingCoreCodes.isEmpty()) break;

            // Take the next missing code
            String code = missingCoreCodes.removeFirst();

            // 5. Find a matching offering from the 'available' list
            //    that doesn't conflict in time and doesn't exceed credit limit
            Optional<CourseOffering> offOpt = findOffering(available, code, currentCredits, slots, studentId);

            if (offOpt.isPresent()) {
                CourseOffering off = offOpt.get();

                // 6. Add the course to the schedule
                slots.add(new ScheduleSlot(
                        CourseType.CORE,
                        off.getId(),
                        off.getBaseCourse().getCredits(),
                        off.getTimes()
                ));

                // 7. Update current credits
                currentCredits += off.getBaseCourse().getCredits();
                added++;
            }
        }

        // Return how many credits we actually added for core
        return added * 3;  // TODO: Remove this hardcoded value (Assuming each core course is 3 credits)
    }

    // =============== 7. GENED REQUIREMENTS ===============
    private int addGenEdIfNeeded(
            List<ScheduleSlot> slots,
            UUID studentId,
            List<CourseOffering> available,
            int currentCredits
    ) {

        // 1. Check if GenEd is already satisfied (clusters + numeric)
        RequirementResult genEdResult = baseDegreeAuditService.checkGeneralEducationRequirementsDetailed(studentId);
        if (genEdResult.isSatisfied()) {
            return 0;  // no GenEd needed
        }

        // 2. Gather the student's *completed* GenEd courses
        List<Course> genEdCompleted = baseDegreeAuditService.getStudentGenEdCompleted(studentId);

        // 3. Ask the clustering service what clusters are still missing
        Set<NeededCluster> neededClusters = genedClusteringService.findNeededClusters(genEdCompleted);
        if (neededClusters.isEmpty()) {
            // Means no cluster combination is missing => effectively the cluster requirement is done
            return 0;
        }

        // 4. Naive approach: pick the first cluster that has missingTotal>0
        for (NeededCluster needed : neededClusters) {
            int needTotal = needed.getMissingTotal();
            if (needTotal <= 0) {
                continue; // cluster is effectively complete
            }

            // We'll gather possible base courses from the theme
            int theme = needed.getTheme();
            int needLower = needed.getMissingLower();
            int needUpper = needed.getMissingUpper();

            // a) Gather all base courses for this theme from the courseService
            List<Course> themeCourses = courseService.getCoursesByTheme(theme);

            // b) Depending on missingLower/missingUpper, filter the list
            // First check if we need a lower - if so, filter to lower
            // If we need an upper, filter to upper
            // If both checks are false, that means we can pick any course upper or lower

            AcademicStanding standing = userService.getAcademicStanding(studentId);
            // If a student is a freshman it should only return lower division courses
            if (standing == AcademicStanding.FRESHMAN) {
                themeCourses = themeCourses.stream()
                        .filter(c -> courseService.isLowerDivision(c.getCode()))
                        .collect(Collectors.toList());
            } else {
                if (needLower > 0) {
                    themeCourses = themeCourses.stream()
                            .filter(c -> courseService.isLowerDivision(c.getCode()))
                            .collect(Collectors.toList());
                }
                else if (needUpper > 0) {
                    themeCourses = themeCourses.stream()
                            .filter(c -> courseService.isUpperDivision(c.getCode()))
                            .collect(Collectors.toList());
                }
            }


            // c) For each base course, try to find an actual offering
            // This fetches the first offering that is found
            // TODO: Replace it so find offering returns all possible offerings
            // TODO: Change so python chooses best one
            for (Course base : themeCourses) {
                Optional<CourseOffering> offOpt = findOffering(
                        available, base.getCode(), currentCredits, slots, studentId);
                if (offOpt.isPresent()) {
                    // Found an offering that satisfies the cluster requirement
                    CourseOffering off = offOpt.get();
                    slots.add(new ScheduleSlot(
                            CourseType.GENED,
                            off.getId(),
                            off.getBaseCourse().getCredits(),
                            off.getTimes()
                    ));
                    return off.getBaseCourse().getCredits();
                }
            }
        }

        // If we reach here, no suitable offering found for any missing cluster
        return 0;
    }

    // =============== 8. TRACK ===============
    private int addTrackIfNeeded(List<ScheduleSlot> slots, UUID studentId, List<CourseOffering> available, int currentCredits) {
        // 1. Check if the student has a track

        boolean atLeastOneTrackDone = baseDegreeAuditService.checkProgramScenarios(studentId).stream()
                .peek(DegreeAuditScenario::canGraduate)
                .anyMatch(DegreeAuditScenario::isSatisfied);

        if (atLeastOneTrackDone) {
            return 0; // Some track is complete
        }

        // 2. Gather the missing track codes for the track that has the most missing courses
        DegreeScenarioType chosenTrack = baseDegreeAuditService.pickChosenTrack(studentId);
        List<String> missingTrackCodes = baseDegreeAuditService.getTrackCourseCodes(chosenTrack);

        // TODO: Replace with Python recommendation
        // 3. Try each missing track code until we find a valid offering
        for (String trackCode : missingTrackCodes) {
            Optional<CourseOffering> offOpt = findOffering(available, trackCode, currentCredits, slots, studentId);
            if (offOpt.isPresent()) {
                CourseOffering off = offOpt.get();
                slots.add(new ScheduleSlot(
                        CourseType.TRACK,
                        off.getId(),
                        off.getBaseCourse().getCredits(),
                        off.getTimes()
                ));
                return off.getBaseCourse().getCredits();
            }
        }

        return 0;
    }

    // =============== 9. FREE ELECTIVE ===============
    private int addFreeElectiveIfNeeded(List<ScheduleSlot> slots, UUID studentId, List<CourseOffering> available, int currentCredits) {
        // 1. Check if the student has any free electives left
        RequirementResult freeElective = baseDegreeAuditService.checkFreeElectiveRequirements(studentId);

        if (freeElective.isSatisfied()) {
            return 0; // No free electives needed
        }

        // Any other course that isn't taken by now can be free elective.
        List<String> missingCodes = freeElective.getPossibleCourseCodes();

        // TODO: Replace with Python recommendation
        // 2. Find an offering from any of the free electives that satisfies the requirements
        for (String courseCode : missingCodes) {
            Optional<CourseOffering> offOpt = findOffering(available, courseCode, currentCredits, slots, studentId);
            if (offOpt.isPresent()) {
                CourseOffering off = offOpt.get();
                slots.add(new ScheduleSlot(
                        CourseType.FREE_ELECTIVE,
                        off.getId(),
                        off.getBaseCourse().getCredits(),
                        off.getTimes()
                ));
                return off.getBaseCourse().getCredits();
            }
        }

        return 0;
    }

    // =============== 10. CAPSTONE ===============
    private int addCapstoneIfPossible(
            List<ScheduleSlot> slots,
            UUID studentId,
            List<CourseOffering> available,
            int currentCredits
    ) {
        // 1. Check if all else is done but capstone
        boolean allElseDone = baseDegreeAuditService.isAllElseDoneButCapstone(studentId);
        if (!allElseDone) {
            // The student isn't ready to do capstone
            return 0;
        }

        // 2. The user can do capstone => find the capstone code
        String capstoneCode = baseDegreeAuditService.getCapstoneCode(); // e.g. "CS296"

        // 3. Attempt to find an offering for this code
        Optional<CourseOffering> offOpt = findOffering(available, capstoneCode, currentCredits, slots, studentId);
        if (offOpt.isPresent()) {
            CourseOffering off = offOpt.get();
            slots.add(new ScheduleSlot(
                    CourseType.CAPSTONE,
                    off.getId(),
                    off.getBaseCourse().getCredits(),  // e.g. 3
                    off.getTimes()
            ));
            return off.getBaseCourse().getCredits();
        }
        return 0;
    }


    // =============== HELPER FUNCTIONS ===============
    private Optional<CourseOffering> findOffering(List<CourseOffering> available, String code, int currentCredits, List<ScheduleSlot> slots, UUID studentId) {
        return available.stream()
                .filter(off -> off.getBaseCourse().getCode().equals(code))
                .filter(off -> currentCredits + off.getBaseCourse().getCredits() <= MAX_CREDITS_PER_REGISTRATION)
                .filter(off -> !hasTimeConflict(off, slots))
                .filter(off -> prerequisitesMet(off, studentId))
                .filter(off -> validAcademicStanding(off, studentId))
                .findFirst();
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
}
