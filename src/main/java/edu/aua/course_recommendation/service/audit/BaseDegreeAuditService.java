package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.model.*;
import edu.aua.course_recommendation.service.auth.UserService;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseDegreeAuditService {

    // Here order matters
    private final static List<String> FOUNDATION_REQUIREMENTS = List.of(
            "FND101", "FND102", // Freshman Seminar 1 & 2
            "FND103", "FND104", // Armenian Language & Literature 1 & 2
            "FND221", "FND222"); // Armenian History 1 & 2

    private final static int REQUIRED_GENED_COUNT = 9;
    private final static int REQUIRED_PHYS_ED_COUNT = 4;
    private static final String FIRST_AID_CODE = "FND152";
    private static final String CIVIL_DEFENSE_CODE = "FND153";
    private static final String PEER_MENTORING_CODE = "PEER001";

    protected final EnrollmentService enrollmentService;
    private final GenedClusteringService genedClusteringService;
    private final CourseService courseService;
    private final UserService userService;

    public abstract RequirementResult checkProgramCore(UUID studentId);
    public abstract List<DegreeAuditScenario> checkProgramScenarios(UUID studentId);
    protected abstract List<String> getCoreAndTrackCourseCodes();
    protected abstract List<String> getCoreCourseCodes();
    public abstract List<String> getTrackCourseCodes(DegreeScenarioType trackType);
    public abstract DegreeScenarioType pickChosenTrack(UUID studentId);
    protected abstract int getRequiredFreeElectiveCount(); // Each degree has different num of requirements
    public abstract String getCapstoneCode(); // Each degree has different capstone code

    public DegreeAuditMultiScenarioResult auditStudentDegreeMultiScenario(UUID studentId) {

        userService.validateStudent(studentId);

        // 1. Build common requirements
        RequirementResult foundation = checkFoundationRequirementsDetailed(studentId);
        RequirementResult peerMentoring = checkPeerMentoringRequirement(studentId);
        RequirementResult phed = checkPhysicalEducationRequirementsDetailed(studentId);
        RequirementResult genEd = checkGeneralEducationRequirementsDetailed(studentId);
        RequirementResult firstAidCivDef = checkFirstAidAndCivilDefense(studentId);
        RequirementResult freeElective = checkFreeElectiveRequirements(studentId);
        RequirementResult capstone = checkCapstoneRequirement(studentId);

        // 2. Check if "Core" for the Major is satisfied
        RequirementResult core = checkProgramCore(studentId);

        List<RequirementResult> commonReqs = List.of(
                peerMentoring,
                firstAidCivDef,
                core,
                foundation,
                phed,
                genEd,
                freeElective,
                capstone);

        // 3. Get scenario-based checks from the child class (Track Information)
        List<DegreeAuditScenario> scenarios = checkProgramScenarios(studentId);

        return new DegreeAuditMultiScenarioResult(commonReqs, scenarios);
    }

    protected List<DegreeAuditScenario> clearPossibleCodesIfAnyTrackSatisfied(List<DegreeAuditScenario> scenarios) {
        boolean anyTrackSatisfied = scenarios.stream()
                .anyMatch(scenario -> scenario.getScenarioRequirements().stream()
                        .anyMatch(RequirementResult::isSatisfied));

        if (anyTrackSatisfied) {
            scenarios.forEach(scenario ->
                    scenario.getScenarioRequirements().forEach(requirement ->
                            requirement.setPossibleCourseCodes(List.of())
                    )
            );
        }

        return scenarios;
    }

    public RequirementResult checkPeerMentoringRequirement(UUID studentId) {
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        boolean isSatisfied = completedCodes.contains(PEER_MENTORING_CODE);

        List<String> completedPeerMentoring = completedCodes.stream()
                .filter(code -> code.equals(PEER_MENTORING_CODE))
                .toList();

        List<String> missingCodes = isSatisfied ? List.of() : List.of(PEER_MENTORING_CODE);
        return new RequirementResult(
                Requirement.PEER_MENTORING,
                isSatisfied,
                missingCodes,
                completedPeerMentoring,
                missingCodes.size()
        );
    }

    public RequirementResult checkFoundationRequirementsDetailed(UUID studentId) {
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        List<String> missing = FOUNDATION_REQUIREMENTS.stream()
                .filter(req -> !completedCodes.contains(req))
                .toList();

        List<String> completedFoundation = FOUNDATION_REQUIREMENTS.stream()
                .filter(completedCodes::contains)
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult(
                Requirement.FOUNDATION,
                isSatisfied,
                List.copyOf(missing),
                completedFoundation,
                missing.size()
        );
    }

    public RequirementResult checkPhysicalEducationRequirementsDetailed(UUID studentId) {

        Set<String> completedPhysedCodes = enrollmentService.getCompletedCourses(studentId).stream()
                .map(Course::getCode)
                .filter(code -> code.startsWith("FND110"))
                .collect(Collectors.toSet());

        int completedCount = completedPhysedCodes.size();
        boolean isSatisfied = (completedCount >= 4);

        List<String> availablePhysedCodes = courseService.getAllCourses().stream()
                .map(Course::getCode)
                .filter(code -> code.startsWith("FND110"))
                .sorted((code1, code2) -> {
                    if (code1.equals("FND110")) return -1;
                    if (code2.equals("FND110")) return 1;
                    return code1.compareTo(code2);
                })
                .toList();

        return new RequirementResult(
                Requirement.PHYSICAL_EDUCATION,
                isSatisfied,
                isSatisfied ? List.of() : availablePhysedCodes,
                completedPhysedCodes.stream().toList(),
                REQUIRED_PHYS_ED_COUNT - completedCount
        );
    }

    public RequirementResult checkGeneralEducationRequirementsDetailed(UUID studentId) {

        Set<String> genEdEligibleCodes = getGenEdEligibleCourseCodes();

        List<Course> genEdCompleted = getStudentGenEdCompleted(studentId);

        boolean clusterSatisfied = genedClusteringService.isGenEdRequirementMet(genEdCompleted);

        List<String> missingCodes = new ArrayList<>();
        if (!clusterSatisfied) {
            Set<NeededCluster> neededClusters = genedClusteringService.findNeededClusters(genEdCompleted);
            List<String> builtMissingCodes = genedClusteringService.buildMissingGenEdCodes(neededClusters, genEdEligibleCodes, genEdCompleted);

            AcademicStanding standing = userService.getAcademicStanding(studentId);
            if (standing == AcademicStanding.FRESHMAN) {
                builtMissingCodes = builtMissingCodes.stream()
                        .filter(courseService::isLowerDivision)
                        .toList();
            }

            missingCodes = builtMissingCodes.stream()
                    .distinct()
                    .collect(Collectors.toList());
        }

        return new RequirementResult(
                Requirement.GENERAL_EDUCATION,
                clusterSatisfied,
                missingCodes,
                genEdCompleted.stream().map(Course::getCode).collect(Collectors.toList()),
                REQUIRED_GENED_COUNT - genEdCompleted.size()
        );
    }

    public Set<NeededCluster> getNeededClusters(UUID studentId) {
        return genedClusteringService.findNeededClusters(enrollmentService.getCompletedCourses(studentId));
    }

    public List<NeededCluster> getGenedStatus(UUID studentId) {
        List<Course> completedCourses = enrollmentService.getCompletedCourses(studentId);
        Set<String> genedPossibleCourseCodes = getGenEdEligibleCourseCodes();

        List<Course> completedGenedCourses = completedCourses.stream()
                .filter(course -> genedPossibleCourseCodes.contains(course.getCode()))
                .toList();

        return genedClusteringService.getGenedClusters(completedGenedCourses);
    }

    public List<GenedClusteringService.ClusterSolution> getClusters(UUID studentId) {
        List<Course> completedCourses = enrollmentService.getCompletedCourses(studentId);
        Set<String> genedPossibleCourseCodes = getGenEdEligibleCourseCodes();

        List<Course> completedGenedCourses = completedCourses.stream()
                .filter(course -> genedPossibleCourseCodes.contains(course.getCode()))
                .toList();

        return genedClusteringService.findPossibleClusterCombinations(completedGenedCourses, 1);
    }

    public RequirementResult checkFirstAidAndCivilDefense(UUID studentId) {

        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        boolean firstAidDone = completedCodes.contains(FIRST_AID_CODE);
        boolean civilDefenseDone = completedCodes.contains(CIVIL_DEFENSE_CODE);

        boolean isSatisfied = (firstAidDone && civilDefenseDone);

        List<String> missing = new ArrayList<>();
        if (!firstAidDone) missing.add(FIRST_AID_CODE);
        if (!civilDefenseDone) missing.add(CIVIL_DEFENSE_CODE);

        return new RequirementResult(
                Requirement.FIRST_AID_AND_CIVIL_DEFENSE,
                isSatisfied,
                missing,
                List.of(FIRST_AID_CODE, CIVIL_DEFENSE_CODE),
                missing.size()
        );
    }

    /**
     * Check for free elective courses.
     * This method makes a few assumptions
     * Free electives = any courses not used for core, foundation, first aid/civil defense, or the chosen track (once that track is finalized).
     * If the student has completed a track (≥5 courses for CS), any other track’s courses can count as free electives.
     * <p>
     * If no track is completed yet:
     *      pick the track with the most completed courses as the “intended” track.
     *      Then all courses from other tracks can be considered free electives.
     *      If the student has zero or equally minimal track progress, default to the “GENERAL” track.
     * If no Gened Requirement (3 valid clusters) isn't completed
     *      we assume that it must be completed FIRST
     *      therefore we say that ALL free electives are still due
     *
     */
    public RequirementResult checkFreeElectiveRequirements(UUID studentId) {

        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        DegreeScenarioType chosenTrack = pickChosenTrack(studentId);

        Set<String> excluded = buildExcludedCodes(chosenTrack, studentId);

        Set<String> allCodes = courseService.getAllCourses().stream()
                .map(Course::getCode)
                .collect(Collectors.toSet());

        Set<String> freeElectiveEligible = new HashSet<>(allCodes);
        freeElectiveEligible.removeAll(excluded);

        Set<String> completedFreeElectives = completedCodes.stream()
                .filter(freeElectiveEligible::contains)
                .collect(Collectors.toSet());

        int requiredFreeElectives = getRequiredFreeElectiveCount();
        int done = completedFreeElectives.size();
        int needed = requiredFreeElectives - done;
        boolean isSatisfied = (needed <= 0);

        List<String> missingCodes = new ArrayList<>();

        if (!isSatisfied) {
            List<String> filtered = freeElectiveEligible.stream()
                    .filter(code -> !completedFreeElectives.contains(code))
                    .collect(Collectors.toList());

            AcademicStanding standing = userService.getAcademicStanding(studentId);
            if (standing == AcademicStanding.FRESHMAN) {
                filtered = filtered.stream()
                        .filter(courseService::isLowerDivision)
                        .toList();
            }

            missingCodes = filtered.stream()
                    .distinct()
                    .collect(Collectors.toList());
        }


        return new RequirementResult(
                Requirement.FREE_ELECTIVE,
                isSatisfied,
                missingCodes,
                completedFreeElectives.stream().toList(),
                needed
        );
    }

    public RequirementResult checkCapstoneRequirement(UUID studentId) {
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);
        String capstoneCode = getCapstoneCode();

        boolean capstoneDone = completedCodes.contains(capstoneCode);

        return new RequirementResult(
                Requirement.CAPSTONE,
                capstoneDone,
                capstoneDone ? List.of() : List.of(capstoneCode),
                capstoneDone ? List.of(capstoneCode) : List.of(),
                capstoneDone ? 0 : 1
        );
    }

    private Set<String> getGenEdEligibleCourseCodes() {

        List<String> coreAndTrackCourseCodes = getCoreAndTrackCourseCodes();

        Set<String> allCourseCodes = courseService.getAllCourses().stream().map(Course::getCode).collect(Collectors.toSet());

        coreAndTrackCourseCodes.forEach(allCourseCodes::remove);

        FOUNDATION_REQUIREMENTS.forEach(allCourseCodes::remove);
        allCourseCodes.remove(FIRST_AID_CODE);
        allCourseCodes.remove(CIVIL_DEFENSE_CODE);

        allCourseCodes.removeIf(code -> code.startsWith("FND110"));

        return allCourseCodes;
    }

    private Set<String> buildExcludedCodes(DegreeScenarioType chosenTrack, UUID studentId) {
        Set<String> excluded = new HashSet<>(FOUNDATION_REQUIREMENTS);
        excluded.add(FIRST_AID_CODE);
        excluded.add(CIVIL_DEFENSE_CODE);
        excluded.add(PEER_MENTORING_CODE);

        Set<String> physEdCodes = courseService.getAllPhysedCourses().stream()
                .map(Course::getCode)
                .collect(Collectors.toSet());

        excluded.addAll(physEdCodes);

        excluded.addAll(getCoreCourseCodes());

        excluded.addAll(getTrackCourseCodes(chosenTrack));

        Set<String> usedForGenEd = findGenEdCoursesUsedForCluster(studentId);
        excluded.addAll(usedForGenEd);

        return excluded;
    }

    private Set<String> findGenEdCoursesUsedForCluster(UUID studentId) {
        List<Course> genEdCompleted = getStudentGenEdCompleted(studentId);

        List<GenedClusteringService.ClusterSolution> solutions =
                genedClusteringService.findPossibleClusterCombinations(genEdCompleted, 1);

        if (solutions.isEmpty()) {
            return genEdCompleted.stream()
                    .map(Course::getCode)
                    .collect(Collectors.toSet());
        }

        GenedClusteringService.ClusterSolution solution = solutions.getFirst();

        Set<String> usedCodes = new HashSet<>();
        for (GenedClusteringService.ClusterChoice choice : solution.getClusterChoices()) {
            for (GenedClusteringService.ClusterChoice.CourseInfo c : choice.getCourses()) {
                usedCodes.add(c.getCode());
            }
        }
        return usedCodes;
    }

    public List<Course> getStudentGenEdCompleted(UUID studentId) {

        List<Course> allStudentCourses = enrollmentService.getCompletedCourses(studentId);

        Set<String> genEdEligibleCodes = getGenEdEligibleCourseCodes();

        return allStudentCourses.stream()
                .filter(course -> genEdEligibleCodes.contains(course.getCode()))
                .collect(Collectors.toList());
    }

    public boolean isAllElseDoneButCapstone(UUID studentId) {
        boolean foundationDone = checkFoundationRequirementsDetailed(studentId).isSatisfied();
        boolean coreDone = checkProgramCore(studentId).isSatisfied();
        boolean trackDone = checkProgramScenarios(studentId).stream()
                .peek(DegreeAuditScenario::canGraduate)
                .anyMatch(DegreeAuditScenario::isSatisfied);
        boolean genEdDone = checkGeneralEducationRequirementsDetailed(studentId).isSatisfied();
        boolean freeElecDone = checkFreeElectiveRequirements(studentId).isSatisfied();
        return (foundationDone && coreDone && trackDone && genEdDone && freeElecDone);
    }

}
