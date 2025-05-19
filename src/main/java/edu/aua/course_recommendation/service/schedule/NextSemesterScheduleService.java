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

        List<CourseOffering> nextSemesterOfferings = courseOfferingService.
                getCourseOfferingsByYearAndSemester(year, semester);

        Set<String> completedCodes = new HashSet<>(enrollmentService.getCompletedCourseCodes(studentId));
        List<CourseOffering> available = nextSemesterOfferings.stream()
                .filter(off -> !completedCodes.contains(off.getBaseCourse().getCode()))
                .toList();

        List<ScheduleSlot> slots = new ArrayList<>();
        int currentCredits = 0;

        // TODO: We'll add a waiver check that sets completed to 4 if the student has a waiver for physed in audit service
        currentCredits += addZeroCreditItems(slots, studentId, available, currentCredits);

        currentCredits += addFoundationIfNeeded(slots, studentId, available, currentCredits);

        currentCredits += addCoreCoursesIfNeeded(slots, studentId, available, currentCredits);

        currentCredits += addGenEdIfNeeded(slots, studentId, available, currentCredits);

        currentCredits += addTrackIfNeeded(slots, studentId, available, currentCredits);

        currentCredits += addFreeElectiveIfNeeded(slots, studentId, available, currentCredits);

        currentCredits += addCapstoneIfPossible(slots, studentId, available, currentCredits);

        return Schedule.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .slots(slots)
                .build();
    }

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

        RequirementResult facd = auditService.checkFirstAidAndCivilDefense(studentId);

        if (!facd.isSatisfied()) {
            // TODO: Doing this since there is no time conflict but ideally logic should be moved to findOffering replaced with a single findOffering call
            for (String missingCode : facd.getPossibleCourseCodes()) {
                Optional<CourseOffering> offering = courseOfferingService.findOfferingByBaseCourseCode(missingCode);

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

        RequirementResult phed = auditService.checkPhysicalEducationRequirementsDetailed(studentId);

        if (!phed.isSatisfied()) {
            for (String peCode : phed.getPossibleCourseCodes()) {
                Optional<CourseOffering> peOffering = scheduleService.findOffering(available, peCode, currentCredits, slots, studentId);
                if (peOffering.isPresent()) {
                    slots.add(new ScheduleSlot(
                            peOffering.get().getId(),
                            peOffering.get().getBaseCourse().getCode(),
                            0,
                            peOffering.get().getTimes()
                    ));
                    log.info("Added PhysEd course: {}", peOffering.get().getBaseCourse().getCode());
                    break;
                }
            }
        }

        RequirementResult peerMentoring = auditService.checkPeerMentoringRequirement(studentId);
        if (!peerMentoring.isSatisfied()) {
            String firstMissingPeer = peerMentoring.getPossibleCourseCodes().stream()
                    .findAny()
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
            return 0;
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);
        RequirementResult foundation = auditService.checkFoundationRequirementsDetailed(studentId);

        if (foundation.isSatisfied()) {
            return 0;
        }

        String firstMissing = foundation.getPossibleCourseCodes().stream()
                .findFirst().orElse(null);

        if (firstMissing == null) {
            return 0;
        }

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

        RequirementResult coreResult = auditService.checkProgramCore(studentId);
        if (coreResult.isSatisfied()) {
            return 0;
        }

        List<String> missingCoreCodes = new ArrayList<>(coreResult.getPossibleCourseCodes());

        int added = 0;
        while (added < 3 && currentCredits < MAX_CREDITS_PER_REGISTRATION) { // TODO: Hardcoded value remove
            if (missingCoreCodes.isEmpty()) break;

            String code = missingCoreCodes.removeFirst();
            System.out.println("Trying to add core course: " + code);

            Optional<CourseOffering> offOpt = scheduleService.findOffering(available, code, currentCredits, slots, studentId);

            if (offOpt.isPresent()) {
                CourseOffering off = offOpt.get();

                slots.add(new ScheduleSlot(
                        off.getId(),
                        off.getBaseCourse().getCode(),
                        off.getBaseCourse().getCredits(),
                        off.getTimes()
                ));
                log.info("Added core course: {} ({}/{} core courses)",
                        off.getBaseCourse().getCode(), added + 1, 3);

                currentCredits += off.getBaseCourse().getCredits();
                added++;
            }
        }

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
            return 0;
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        RequirementResult genEdResult = auditService.checkGeneralEducationRequirementsDetailed(studentId);
        if (genEdResult.isSatisfied()) {
            return 0;
        }

        List<Course> genEdCompleted = auditService.getStudentGenEdCompleted(studentId);

        Set<NeededCluster> neededClusters = genedClusteringService.findNeededClusters(genEdCompleted);
        if (neededClusters.isEmpty()) {
            return 0;
        }

        neededClusters.forEach(cluster -> System.out.printf("Theme %d: needs %d total courses (Lower: %d, Upper: %d)%n",
                cluster.getTheme(), cluster.getMissingTotal(), cluster.getMissingLower(), cluster.getMissingUpper()));

        List<NeededCluster> sortedClusters = neededClusters.stream()
                .filter(cluster -> cluster.getMissingTotal() > 0)
                .sorted(Comparator.comparingInt(NeededCluster::getMissingTotal))
                .toList();

        for (NeededCluster needed : sortedClusters) {
            int theme = needed.getTheme();
            int needLower = needed.getMissingLower();
            int needUpper = needed.getMissingUpper();

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

            AcademicStanding standing = userService.getAcademicStanding(studentId);
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

            log.info("Theme {} courses: {}", theme, themeCourses);

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
                }

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
            return 0;
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        boolean atLeastOneTrackDone = auditService.checkProgramScenarios(studentId).stream()
                .peek(DegreeAuditScenario::canGraduate)
                .anyMatch(DegreeAuditScenario::isSatisfied);

        if (atLeastOneTrackDone) {
            return 0;
        }

        DegreeScenarioType chosenTrack = auditService.pickChosenTrack(studentId);
        List<String> missingTrackCodes = auditService.getTrackCourseCodes(chosenTrack);

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
            return 0;
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        RequirementResult freeElective = auditService.checkFreeElectiveRequirements(studentId);

        if (freeElective.isSatisfied()) {
            return 0;
        }

        List<String> possibleElectives = freeElective.getPossibleCourseCodes();

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
            }

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
            return 0;
        }

        BaseDegreeAuditService auditService = degreeAuditServiceRouter.getServiceForStudent(studentId);

        Set<String> simulatedCompleted = getSimulatedCompletedCourseCodes(studentId, slots);
        enrollmentService.setSimulatedCompletedCourses(simulatedCompleted);

        boolean allElseDone = auditService.isAllElseDoneButCapstone(studentId);

        enrollmentService.clearSimulatedCompletedCourses();

        if (!allElseDone) {
            return 0;
        }

        String capstoneCode = auditService.getCapstoneCode();

        Optional<CourseOffering> offOpt = scheduleService.findOffering(available, capstoneCode, currentCredits, slots, studentId);
        if (offOpt.isPresent()) {
            CourseOffering off = offOpt.get();
            slots.add(new ScheduleSlot(
                    off.getId(),
                    off.getBaseCourse().getCode(),
                    off.getBaseCourse().getCredits(),
                    off.getTimes()
            ));
            log.info("Added capstone course: {}", off.getBaseCourse().getCode());
            return off.getBaseCourse().getCredits();
        }
        return 0;
    }

    private Set<String> getSimulatedCompletedCourseCodes(UUID studentId, List<ScheduleSlot> slots) {
        Set<String> union = new HashSet<>(enrollmentService.getCompletedCourseCodes(studentId));
        for (ScheduleSlot slot : slots) {
            Course baseCourse = courseOfferingService.getBaseCourseByOfferingId(slot.getOfferingId());
            union.add(baseCourse.getCode());
        }
        return union;
    }

}
