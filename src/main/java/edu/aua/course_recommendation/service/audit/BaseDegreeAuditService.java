package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.*;
import edu.aua.course_recommendation.repository.CourseOfferingRepository;
import edu.aua.course_recommendation.service.auth.UserService;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
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

        //userService.validateStudent(studentId);

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

    public RequirementResult checkPeerMentoringRequirement(UUID studentId) {
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        // Check if the student has completed the Peer Mentoring course
        boolean isSatisfied = completedCodes.contains("PEER001"); // TODO: Remove hardcoded code

        // If not satisfied, return the missing code
        List<String> missingCodes = isSatisfied ? List.of() : List.of("PEER001");
        return new RequirementResult(
                Requirement.PEER_MENTORING,
                isSatisfied,
                missingCodes,
                missingCodes.size()
        );
    }

    public RequirementResult checkFoundationRequirementsDetailed(UUID studentId) {
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        // Which of the foundation courses are missing?
        List<String> missing = FOUNDATION_REQUIREMENTS.stream()
                .filter(req -> !completedCodes.contains(req))
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult(
                Requirement.FOUNDATION,
                isSatisfied,
                List.copyOf(missing),
                missing.size()
        );
    }

    public RequirementResult checkPhysicalEducationRequirementsDetailed(UUID studentId) {

        // How many courses beginning with "FND110" are completed?
        Set<String> completedPhysedCodes = enrollmentService.getCompletedCourses(studentId).stream()
                .map(Course::getCode)
                .filter(code -> code.startsWith("FND110"))
                .collect(Collectors.toSet());

        int completedCount = completedPhysedCodes.size();
        boolean isSatisfied = (completedCount >= 4);

        List<String> availablePhysedCodes = courseService.getAllCourses().stream()
                .map(Course::getCode)
                .filter(code -> code.startsWith("FND110"))
                .filter(code -> !completedPhysedCodes.contains(code))
                .toList();
        return new RequirementResult(
                Requirement.PHYSICAL_EDUCATION,
                isSatisfied,
                availablePhysedCodes,
                REQUIRED_PHYS_ED_COUNT - completedCount
        );
    }

    /**
     * Check for general education requirements
     * Returns if the student completed BREATH REQUIREMENTS (9 Courses)
     * THIS IS MORE LIKE BREATH REQUIREMENTS
     * Actual "General Education" takes foundation courses into account as well
     * Might need to rename
     * <p>
     * For each degree a course is considered gen-ed if it's not core or track elective
     */
    public RequirementResult checkGeneralEducationRequirementsDetailed(UUID studentId) {
        // 1. Fetch *all* courses the student has completed

        // 2. Get eligible gened courses for that major
        Set<String> genEdEligibleCodes = getGenEdEligibleCourseCodes();

        // Get completed Geneds
        List<Course> genEdCompleted = getStudentGenEdCompleted(studentId);

        // 5. Check if clusters are completed
        //    This takes into the 9 course requirement as the method won't return true if there are no 9 courses
        //    e.g., call isGenEdRequirementMet(genEdCompleted) for your backtracking approach
        boolean clusterSatisfied = genedClusteringService.isGenEdRequirementMet(genEdCompleted);

        // 6. If not satisfied, define "missingCodes"
        //    which is all GenEd codes minus the ones they've completed
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

        // 7. Return a RequirementResult
        // Returns potential gen-eds the student might need to take the complete the clusters
        return new RequirementResult(
                Requirement.GENERAL_EDUCATION,
                clusterSatisfied,
                missingCodes,
                REQUIRED_GENED_COUNT - genEdCompleted.size()
        );
    }

    public Set<NeededCluster> getNeededClusters(UUID studentId) {
        return genedClusteringService.findNeededClusters(enrollmentService.getCompletedCourses(studentId));
    }

    public List<GenedClusteringService.ClusterSolution> getClusters(UUID studentId) {
        return genedClusteringService.findPossibleClusterCombinations(enrollmentService.getCompletedCourses(studentId), 5);
    }

    public RequirementResult checkFirstAidAndCivilDefense(UUID studentId) {
        // 1. Student’s completed codes
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        // 2. Check if the codes "FND152" and "FND153" are in there
        boolean firstAidDone = completedCodes.contains(FIRST_AID_CODE);
        boolean civilDefenseDone = completedCodes.contains(CIVIL_DEFENSE_CODE);

        // 3. If both are done => isSatisfied = true
        boolean isSatisfied = (firstAidDone && civilDefenseDone);

        // 4. For the missing codes, we store them in a set
        List<String> missing = new ArrayList<>();
        if (!firstAidDone) missing.add(FIRST_AID_CODE);
        if (!civilDefenseDone) missing.add(CIVIL_DEFENSE_CODE);

        // 5. Possibly you store how many are missing in the 'count' field
        return new RequirementResult(
                Requirement.FIRST_AID_AND_CIVIL_DEFENSE,
                isSatisfied,
                missing,
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
    // TODO: Return ALL Free electives if no valid gened cluster
    public RequirementResult checkFreeElectiveRequirements(UUID studentId) {

        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        // 1. Which track is “chosen” or “completed”?
        //    We'll figure out which track the student has finished or is closest to finishing
        DegreeScenarioType chosenTrack = pickChosenTrack(studentId);

        // 2. Gather the set of codes that are "excluded" from free electives:
        //    foundation, firstAid, civDefense, PE, core, chosenTrack courses
        Set<String> excluded = buildExcludedCodes(chosenTrack, studentId);

        // 3. All courses from the entire system
        Set<String> allCodes = courseService.getAllCourses().stream()
                .map(Course::getCode)
                .collect(Collectors.toSet());

        // 4. freeElectiveEligible = allCodes - excluded
        Set<String> freeElectiveEligible = new HashSet<>(allCodes);
        freeElectiveEligible.removeAll(excluded);

        // 5. Student's completed free electives = intersection of completedCodes with freeElectiveEligible
        Set<String> completedFreeElectives = completedCodes.stream()
                .filter(freeElectiveEligible::contains)
                .collect(Collectors.toSet());

        // 6. Compare with the required free elective count for the major
        int requiredFreeElectives = getRequiredFreeElectiveCount();
        int done = completedFreeElectives.size();
        int needed = requiredFreeElectives - done;
        boolean isSatisfied = (needed <= 0);

        // 7. If not satisfied, gather "missing" = freeElectiveEligible - completedFreeElectives
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
                needed
        );
    }

    public RequirementResult checkCapstoneRequirement(UUID studentId) {
        // 1. Check if the student already completed the capstone
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);
        String capstoneCode = getCapstoneCode();

        boolean capstoneDone = completedCodes.contains(capstoneCode);

        return new RequirementResult(
                Requirement.CAPSTONE,
                capstoneDone,
                capstoneDone ? List.of() : List.of(capstoneCode),
                capstoneDone ? 0 : 1
        );
    }

    private Set<String> getGenEdEligibleCourseCodes() {

        // Get all Track or Core courses for that Major
        List<String> coreAndTrackCourseCodes = getCoreAndTrackCourseCodes();

        // Get all available base courses
        Set<String> allCourseCodes = courseService.getAllCourses().stream().map(Course::getCode).collect(Collectors.toSet());

        // Remove courses that are a CORE or Track
        coreAndTrackCourseCodes.forEach(allCourseCodes::remove);

        // Remove Foundation
        // Remove First Aid & Civil Defense as these cannot be counted towards Gened.
        FOUNDATION_REQUIREMENTS.forEach(allCourseCodes::remove);
        allCourseCodes.remove(FIRST_AID_CODE);
        allCourseCodes.remove(CIVIL_DEFENSE_CODE);

        // Remove Physed Courses
        allCourseCodes.removeIf(code -> code.startsWith("FND110"));

        return allCourseCodes;
    }

    // Helper method to figure out which courses CANNOT be counted towards free elective.
    private Set<String> buildExcludedCodes(DegreeScenarioType chosenTrack, UUID studentId) {
        // Exclude Foundation Courses
        Set<String> excluded = new HashSet<>(FOUNDATION_REQUIREMENTS);
        excluded.add(FIRST_AID_CODE);
        excluded.add(CIVIL_DEFENSE_CODE);
        excluded.add(PEER_MENTORING_CODE);

        // Exclude physed courses
        Set<String> physEdCodes = courseService.getAllPhysedCourses().stream()
                .map(Course::getCode)
                .collect(Collectors.toSet());

        excluded.addAll(physEdCodes);

        // Exclude Core Courses
        excluded.addAll(getCoreCourseCodes());

        // Exclude "chosen" track courses
        excluded.addAll(getTrackCourseCodes(chosenTrack));

        // Exclude Gened courses that were used to complete breadth requirement.
        Set<String> usedForGenEd = findGenEdCoursesUsedForCluster(studentId);
        excluded.addAll(usedForGenEd);

        return excluded;
    }

    private Set<String> findGenEdCoursesUsedForCluster(UUID studentId) {
        List<Course> genEdCompleted = getStudentGenEdCompleted(studentId);

        List<GenedClusteringService.ClusterSolution> solutions =
                genedClusteringService.findPossibleClusterCombinations(genEdCompleted, 1);

        if (solutions.isEmpty()) {
            // no valid cluster arrangement => the student has not completed gened requirement yet
            // whatever course there is exclude from free elective as these need to be used for gened
            // and gened needs to be completed first.
            return genEdCompleted.stream()
                    .map(Course::getCode)
                    .collect(Collectors.toSet());
        }

        // 3. If we do have a valid solution, only exclude the courses actually used in that cluster solution
        // No difference which courses used to form the clusters, which taken for free elective
        GenedClusteringService.ClusterSolution solution = solutions.getFirst();

        // 4. Gather all codes used in that solution
        Set<String> usedCodes = new HashSet<>();
        for (GenedClusteringService.ClusterChoice choice : solution.getClusterChoices()) {
            for (Course c : choice.getCourses()) {
                usedCodes.add(c.getCode());
            }
        }
        return usedCodes;
    }

    public List<Course> getStudentGenEdCompleted(UUID studentId) {
        // 1. Get all completed courses
        List<Course> allStudentCourses = enrollmentService.getCompletedCourses(studentId);

        // 2. Get eligible gened courses for that major
        Set<String> genEdEligibleCodes = getGenEdEligibleCourseCodes();

        // 3. Filter to only GenEd eligible courses
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
