package edu.aua.course_recommendation.service.schedule;

import edu.aua.course_recommendation.dto.response.RecommendationDto;
import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.entity.Schedule;
import edu.aua.course_recommendation.entity.ScheduleSlot;
import edu.aua.course_recommendation.model.*;
import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.audit.DegreeAuditServiceRouter;
import edu.aua.course_recommendation.service.audit.GenedClusteringService;
import edu.aua.course_recommendation.service.auth.UserService;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import edu.aua.course_recommendation.util.AcademicCalendarUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static edu.aua.course_recommendation.service.schedule.ScheduleService.MAX_CREDITS_PER_REGISTRATION;

@Slf4j
@Service
@AllArgsConstructor
public class NextSemesterScheduleService {

    private final EnrollmentService enrollmentService;
    private final DegreeAuditServiceRouter degreeAuditServiceRouter;
    private final CourseOfferingService courseOfferingService;
    private final GenedClusteringService genedClusteringService;
    private final CourseService courseService;
    private final UserService userService;
    private final ScheduleService scheduleService;
    private final PythonService pythonService;

    public Schedule generateNextSemesterCustom(UUID studentId, String year, String semester) {
        userService.validateStudent(studentId);

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
        return Schedule.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .slots(slots)
                .build();
    }

    /**
     * Generates a schedule for the next semester based on the current date.
     * The semester is determined by the current date and the academic calendar.
     *
     * @param studentId The ID of the student for whom to generate the schedule.
     * @return A Schedule object containing the generated schedule.
     */
    public Schedule generateNextSemester(UUID studentId) {
        String[] nextPeriod = AcademicCalendarUtil.getNextAcademicPeriod();
        String year = nextPeriod[0];
        String semester = nextPeriod[1];
        log.info("Generating schedule for student {} for {}-{}", studentId, year, semester);

        return generateNextSemesterCustom(studentId, year, semester);
    }


    // =============== 4. ZERO-CREDIT ITEMS ===============
    // =============== FIRST AID, CIVIL DEFENSE, PHYSICAL EDUCATION AND PEER MENTORING ===============
    private int addZeroCreditItems(List<ScheduleSlot> slots, UUID studentId,
                                   List<CourseOffering> available,
                                   int currentCredits) {

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        // Check First Aid and Civil Defense
        RequirementResult facd = auditService.checkFirstAidAndCivilDefense(studentId);

        if (!facd.isSatisfied()) {
            // Doing this since there is no time conflict but ideally logic should be moved to findOffering replaced with a single findOffering call
            for (String missingCode : facd.getPossibleCourseCodes()) {
                Optional<CourseOffering> offering = courseOfferingService.findOfferingByBaseCourseCode(missingCode);

                // Only add slot if offering exists
                offering.ifPresent(courseOffering -> {
                    slots.add(new ScheduleSlot(
                            courseOffering.getId(),
                            courseOffering.getBaseCourse().getCode(),
                            0,
                            "N/A"
                    ));
                    log.info("Added zero-credit course: {} (First Aid/Civil Defense)", courseOffering.getBaseCourse().getCode());
                });
            }
        }

        // PhysEd check
        RequirementResult phed = auditService.checkPhysicalEducationRequirementsDetailed(studentId);

        if (!phed.isSatisfied()) {
            // Try to find an offering for the first missing PE code
            String firstMissingPE = phed.getPossibleCourseCodes().stream()
                    .findFirst()
                    .orElse(null);

            if (firstMissingPE != null) {
                Optional<CourseOffering> peOffering = scheduleService.findOffering(available, firstMissingPE, currentCredits, slots, studentId);
                peOffering.ifPresent(courseOffering -> {
                    slots.add(new ScheduleSlot(
                            courseOffering.getId(),
                            courseOffering.getBaseCourse().getCode(),
                            0,
                            courseOffering.getTimes()
                    ));
                    log.info("Added PhysEd course: {}", courseOffering.getBaseCourse().getCode());
                });
            }
        }

        // Peer mentoring check
        RequirementResult peerMentoring = auditService.checkPeerMentoringRequirement(studentId);
        if (!peerMentoring.isSatisfied()) {
            // Try to find an offering for the first missing Peer Mentoring code
            String firstMissingPeer = peerMentoring.getPossibleCourseCodes().stream()
                    .findFirst()
                    .orElse(null);

            if (firstMissingPeer != null) {
                Optional<CourseOffering> peerOffering = scheduleService.findOffering(available, firstMissingPeer, currentCredits, slots, studentId);
                peerOffering.ifPresent(courseOffering -> {
                    slots.add(new ScheduleSlot(
                            courseOffering.getId(),
                            courseOffering.getBaseCourse().getCode(),
                            0,
                            courseOffering.getTimes()
                    ));
                    log.info("Added Peer Mentoring course: {}", courseOffering.getBaseCourse().getCode());
                });
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
        if (currentCredits >= MAX_CREDITS_PER_REGISTRATION) {
            return 0; // No room for foundation
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);
        RequirementResult foundation = auditService.checkFoundationRequirementsDetailed(studentId);

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
        Optional<CourseOffering> off = scheduleService.findOffering(available, firstMissing, currentCredits, slots, studentId);
        if (off.isPresent()) {
            slots.add(new ScheduleSlot(
                    off.get().getId(),
                    off.get().getBaseCourse().getCode(),
                    3,
                    off.get().getTimes()
            ));
            log.info("Added foundation course: {}", off.get().getBaseCourse().getCode());
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
        if (currentCredits >= MAX_CREDITS_PER_REGISTRATION) {
            return 0;
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        // 1. Check if the student still needs any core courses
        RequirementResult coreResult = auditService.checkProgramCore(studentId);
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
            System.out.println("Trying to add core course: " + code);

            // 5. Find a matching offering from the 'available' list
            //    that doesn't conflict in time and doesn't exceed credit limit
            Optional<CourseOffering> offOpt = scheduleService.findOffering(available, code, currentCredits, slots, studentId);

            if (offOpt.isPresent()) {
                CourseOffering off = offOpt.get();

                // 6. Add the course to the schedule
                slots.add(new ScheduleSlot(
                        off.getId(),
                        off.getBaseCourse().getCode(),
                        off.getBaseCourse().getCredits(),
                        off.getTimes()
                ));
                log.info("Added core course: {} ({}/{} core courses)",
                        off.getBaseCourse().getCode(), added + 1, 3);

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
        if (currentCredits >= MAX_CREDITS_PER_REGISTRATION) {
            return 0; // No room for GenEd
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        // 1. Check if GenEd is already satisfied (clusters + numeric)
        RequirementResult genEdResult = auditService.checkGeneralEducationRequirementsDetailed(studentId);
        if (genEdResult.isSatisfied()) {
            return 0;  // no GenEd needed
        }

        // 2. Gather the student's *completed* GenEd courses
        List<Course> genEdCompleted = auditService.getStudentGenEdCompleted(studentId);

        // 3. Ask the clustering service what clusters are still missing
        Set<NeededCluster> neededClusters = genedClusteringService.findNeededClusters(genEdCompleted);
        if (neededClusters.isEmpty()) {
            // Means no cluster combination is missing => effectively the cluster requirement is done
            return 0;
        }

        neededClusters.forEach(cluster -> System.out.printf("Theme %d: needs %d total courses (Lower: %d, Upper: %d)%n",
                cluster.getTheme(), cluster.getMissingTotal(), cluster.getMissingLower(), cluster.getMissingUpper()));

        // 4. Sort clusters by missing total courses (prioritizing clusters with fewer missing courses)
        List<NeededCluster> sortedClusters = neededClusters.stream()
                .filter(cluster -> cluster.getMissingTotal() > 0)  // Only consider clusters that need courses
                .sorted(Comparator.comparingInt(NeededCluster::getMissingTotal))  // Sort by missing total
                .toList();

        // Process each cluster in order (from fewest to most missing courses)
        for (NeededCluster needed : sortedClusters) {
            int theme = needed.getTheme();
            int needLower = needed.getMissingLower();
            int needUpper = needed.getMissingUpper();

            // a) Gather all base courses for this theme from the courseService
            List<String> possibleGeneds = auditService.checkGeneralEducationRequirementsDetailed(studentId).getPossibleCourseCodes();
            System.out.println("Possible GenEd courses: " + possibleGeneds);
            List<Course> themeCourses = courseService.getCoursesByTheme(theme)
                    .stream()
                    .filter(course -> possibleGeneds.contains(course.getCode()))
                    // Let's prioritize humanities courses first
                    .sorted((c1, c2) -> {
                        boolean c1IsHumanities = c1.getCode().startsWith("CHSS");
                        boolean c2IsHumanities = c2.getCode().startsWith("CHSS");

                        if (c1IsHumanities && !c2IsHumanities) return -1;
                        if (!c1IsHumanities && c2IsHumanities) return 1;
                        return 0;
                    })
                    .collect(Collectors.toList());

            // b) Filter based on division level needs and academic standing
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
                } else if (needUpper > 0) {
                    themeCourses = themeCourses.stream()
                            .filter(c -> courseService.isUpperDivision(c.getCode()))
                            .collect(Collectors.toList());
                }
            }

            // c) Try recommendations from Python if we have completed courses
            List<Course> completedCourses = enrollmentService.getCompletedCourses(studentId);

            if (!completedCourses.isEmpty()) {
                List<RecommendationDto> pythonRecommends = new ArrayList<>();
                try {
                    pythonRecommends = pythonService.getRecommendationsWithPassedCourses(
                            completedCourses.stream().map(Course::getCode).toList(),
                            themeCourses.stream().map(Course::getCode).toList()
                    );
                    log.debug("Received {} Python recommendations for student {} in theme {}",
                            pythonRecommends.size(), studentId, theme);
                } catch (Exception e) {
                    log.error("Failed to get Python recommendations for theme {}: {}", theme, e.getMessage(), e);
                    // Empty list will trigger the fallback mechanism
                }

                // Try to find offerings for each recommendation first
                for (RecommendationDto recommendation : pythonRecommends) {
                    Optional<CourseOffering> offOpt = scheduleService.findOffering(
                            available, recommendation.courseCode(), currentCredits, slots, studentId);
                    if (offOpt.isPresent()) {
                        // Found an offering that matches a recommendation
                        CourseOffering off = offOpt.get();
                        slots.add(new ScheduleSlot(
                                off.getId(),
                                off.getBaseCourse().getCode(),
                                off.getBaseCourse().getCredits(),
                                off.getTimes()
                        ));
                        log.info("Added GenEd course from recommendation: {} (Theme {}, Score: {}, Missing: {})",
                                off.getBaseCourse().getCode(), theme, recommendation.score(), needed.getMissingTotal());
                        return off.getBaseCourse().getCredits();
                    }
                }
            }

            // Fall back to picking the first one that matches if recommendations aren't available
            // or if no offerings were found for any recommendation
            for (Course base : themeCourses) {
                Optional<CourseOffering> offOpt = scheduleService.findOffering(
                        available, base.getCode(), currentCredits, slots, studentId);
                if (offOpt.isPresent()) {
                    // Found an offering that satisfies the cluster requirement
                    CourseOffering off = offOpt.get();
                    slots.add(new ScheduleSlot(
                            off.getId(),
                            off.getBaseCourse().getCode(),
                            off.getBaseCourse().getCredits(),
                            off.getTimes()
                    ));
                    log.info("Added GenEd course: {} (Theme {}, Missing: {})",
                            off.getBaseCourse().getCode(), theme, needed.getMissingTotal());
                    return off.getBaseCourse().getCredits();
                }
            }
        }

        // If we reach here, no suitable offering found for any missing cluster
        return 0;
    }

    // =============== 8. TRACK ===============
    private int addTrackIfNeeded(List<ScheduleSlot> slots, UUID studentId, List<CourseOffering> available, int currentCredits) {
        if (currentCredits >= MAX_CREDITS_PER_REGISTRATION) {
            return 0; // No room for track
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        // 1. Check if the student has a track
        boolean atLeastOneTrackDone = auditService.checkProgramScenarios(studentId).stream()
                .peek(DegreeAuditScenario::canGraduate)
                .anyMatch(DegreeAuditScenario::isSatisfied);

        if (atLeastOneTrackDone) {
            return 0; // Some track is already complete
        }

        // 2. Gather the missing track codes for the track that has the most missing courses
        DegreeScenarioType chosenTrack = auditService.pickChosenTrack(studentId);
        List<String> missingTrackCodes = auditService.getTrackCourseCodes(chosenTrack);

        // 3. Try each missing track code until we find a valid offering
        for (String trackCode : missingTrackCodes) {
            Optional<CourseOffering> offOpt = scheduleService.findOffering(available, trackCode, currentCredits, slots, studentId);
            if (offOpt.isPresent()) {
                CourseOffering off = offOpt.get();
                slots.add(new ScheduleSlot(
                        off.getId(),
                        off.getBaseCourse().getCode(),
                        off.getBaseCourse().getCredits(),
                        off.getTimes()
                ));
                log.info("Added track course: {} (Track: {})",
                        off.getBaseCourse().getCode(), chosenTrack);
                return off.getBaseCourse().getCredits();
            }
        }

        return 0;
    }

    // =============== 9. FREE ELECTIVE ===============
    private int addFreeElectiveIfNeeded(List<ScheduleSlot> slots, UUID studentId, List<CourseOffering> available, int currentCredits) {
        if (currentCredits >= MAX_CREDITS_PER_REGISTRATION) {
            return 0; // No room for free elective
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        // 1. Check if the student has any free electives left
        RequirementResult freeElective = auditService.checkFreeElectiveRequirements(studentId);

        if (freeElective.isSatisfied()) {
            return 0; // No free electives needed
        }

        // Any other course that isn't taken by now can be free elective.
        List<String> possibleElectives = freeElective.getPossibleCourseCodes();

        // 2. First try Python recommendations if student has completed courses
        List<Course> completedCourses = enrollmentService.getCompletedCourses(studentId);

        if (!completedCourses.isEmpty()) {

            List<RecommendationDto> pythonRecommends = new ArrayList<>();
            try {
                pythonRecommends = pythonService.getRecommendationsWithPassedCourses(
                        completedCourses.stream().map(Course::getCode).toList(),
                        possibleElectives
                );
                log.debug("Received {} Python recommendations for free electives for student {}", pythonRecommends.size(), studentId);
            } catch (Exception e) {
                log.error("Failed to get Python recommendations for free electives: {}", e.getMessage(), e);
                // Empty list will trigger the fallback mechanism
            }

            // Try to find offerings for each recommendation first
            for (RecommendationDto recommendation : pythonRecommends) {
                Optional<CourseOffering> offOpt = scheduleService.findOffering(
                        available, recommendation.courseCode(), currentCredits, slots, studentId);
                if (offOpt.isPresent()) {
                    CourseOffering off = offOpt.get();
                    slots.add(new ScheduleSlot(
                            off.getId(),
                            off.getBaseCourse().getCode(),
                            off.getBaseCourse().getCredits(),
                            off.getTimes()
                    ));
                    log.info("Added free elective from recommendation: {} (Score: {})",
                            off.getBaseCourse().getCode(), recommendation.score());
                    return off.getBaseCourse().getCredits();
                }
            }
        }

        // Fall back to picking the first available elective if recommendations aren't available
        // or if no offerings were found for any recommendation
        for (String courseCode : possibleElectives) {
            Optional<CourseOffering> offOpt = scheduleService.findOffering(available, courseCode, currentCredits, slots, studentId);
            if (offOpt.isPresent()) {
                CourseOffering off = offOpt.get();
                slots.add(new ScheduleSlot(
                        off.getId(),
                        off.getBaseCourse().getCode(),
                        off.getBaseCourse().getCredits(),
                        off.getTimes()
                ));
                log.info("Added free elective: {}", off.getBaseCourse().getCode());
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
        if (currentCredits >= MAX_CREDITS_PER_REGISTRATION) {
            return 0; // No room for capstone
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        // 0. Add the courses in slots to the students completed courses temporarily
        // Can the student graduate assuming they will pass whatever they're taking this semester?
        Set<String> simulatedCompleted = getSimulatedCompletedCourseCodes(studentId, slots);
        enrollmentService.setSimulatedCompletedCourses(simulatedCompleted);

        // 1. Check if all else is done but capstone
        boolean allElseDone = auditService.isAllElseDoneButCapstone(studentId);

        enrollmentService.clearSimulatedCompletedCourses();

        if (!allElseDone) {
            // The student isn't ready to do capstone
            return 0;
        }

        // 2. The user can do capstone => find the capstone code
        String capstoneCode = auditService.getCapstoneCode(); // e.g. "CS296"

        // 3. Attempt to find an offering for this code
        Optional<CourseOffering> offOpt = scheduleService.findOffering(available, capstoneCode, currentCredits, slots, studentId);
        if (offOpt.isPresent()) {
            CourseOffering off = offOpt.get();
            slots.add(new ScheduleSlot(
                    off.getId(),
                    off.getBaseCourse().getCode(),
                    off.getBaseCourse().getCredits(),  // e.g. 3
                    off.getTimes()
            ));
            log.info("Added capstone course: {}", off.getBaseCourse().getCode());
            return off.getBaseCourse().getCredits();
        }
        return 0;
    }

    // Used to temporarily add current semesters courses to completed courses
    // To determine if the student can SHOULD take capstone
    private Set<String> getSimulatedCompletedCourseCodes(UUID studentId, List<ScheduleSlot> slots) {
        Set<String> union = new HashSet<>(enrollmentService.getCompletedCourseCodes(studentId));
        for (ScheduleSlot slot : slots) {
            // Assuming each slot has a valid offeringId, and getBaseCourseByOfferingId will throw if not found.
            Course baseCourse = courseOfferingService.getBaseCourseByOfferingId(slot.getOfferingId());
            union.add(baseCourse.getCode());
        }
        return union;
    }

}
