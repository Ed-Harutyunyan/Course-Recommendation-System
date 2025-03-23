package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.model.DegreeAuditMultiScenarioResult;
import edu.aua.course_recommendation.model.DegreeAuditScenario;
import edu.aua.course_recommendation.model.NeededCluster;
import edu.aua.course_recommendation.model.RequirementResult;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseDegreeAuditService {

    private final static List<String> FOUNDATION_REQUIREMENTS = List.of(
            "FND101", "FND102", // Freshman Seminar 1 & 2
            "FND103", "FND104", // Armenian Language & Literature 1 & 2
            "FND221", "FND222"); // Armenian History 1 & 2

    protected final EnrollmentService enrollmentService;
    private final GenedClusteringService genedClusteringService;

    protected abstract List<DegreeAuditScenario> checkProgramScenarios(UUID studentId);

    /**
     * Each child class returns the set of course codes that are
     * NOT considered GenEd for that department. (i.e. major’s core/track codes)
     */
    protected abstract Set<String> getNonGenEdCourseCodesForMajor();


    public DegreeAuditMultiScenarioResult auditStudentDegreeMultiScenario(UUID studentId) {

        // 1. Build common requirements
        RequirementResult foundation = checkFoundationRequirementsDetailed(studentId);
        RequirementResult phed = checkPhysicalEducationRequirementsDetailed(studentId);
        RequirementResult genEd = checkGeneralEducationRequirementsDetailed(studentId);

        List<RequirementResult> commonReqs = List.of(foundation, phed, genEd);

        // 2. Get scenario-based checks from the child class
        List<DegreeAuditScenario> scenarios = checkProgramScenarios(studentId);

        return new DegreeAuditMultiScenarioResult(commonReqs, scenarios);
    }

    protected RequirementResult checkFoundationRequirementsDetailed(UUID studentId) {
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        // Which of the foundation courses are missing?
        List<String> missing = FOUNDATION_REQUIREMENTS.stream()
                .filter(req -> !completedCodes.contains(req))
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult(
                "Foundation Requirements",
                isSatisfied,
                Set.copyOf(missing) // missingCourseCodes
        );
    }

    protected RequirementResult checkPhysicalEducationRequirementsDetailed(UUID studentId) {
        long completedCount = enrollmentService.getCompletedCourseCodes(studentId).stream()
                .filter(code -> code.startsWith("FND110"))
                .count();

        boolean isSatisfied = (completedCount >= 4);
        if (isSatisfied) {
            return new RequirementResult("Physical Education", true, Set.of());
        } else {
            int needed = (int)(4 - completedCount);
            return new RequirementResult(
                    "Physical Education",
                    false,
                    Set.of("FND110")
            );
        }
    }

    public boolean checkGened(UUID studentId) {
        List<Course> completedCourses = enrollmentService.getCompletedCourses(studentId);
        return genedClusteringService.isGenEdRequirementMet(completedCourses);
    }

    /*
     * TODO: Not implemented yet.
     * Check for general education requirements
     * Since this requirement can be completed in different ways
     * Not sure if this should return one possible way or all possible ways (probably all)
     *
     * For each degree a course is gen-ed if its not core or track elective
     */
    protected RequirementResult checkGeneralEducationRequirementsDetailed(UUID studentId) {
        // 1. Fetch the student's completed courses
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        // 2. Get the department-specific set of non-GenEd codes
        Set<String> nonGenEdCodes = getNonGenEdCourseCodesForMajor();

        // 3. Filter out non-GenEd => the remainder are GenEd courses
        List<String> genEdCompleted = completedCodes.stream()
                .filter(code -> !nonGenEdCodes.contains(code))
                .toList();

        log.info(genEdCompleted.toString());
        // Example: say we need 5 GenEd courses
        int requiredGenEdCount = 5;
        int missingCount = requiredGenEdCount - genEdCompleted.size();
        if (missingCount <= 0) {
            // Satisfied
            return new RequirementResult(
                    "General Education",
                    true,
                    Set.of()
            );
        } else {
            // Missing some
            return new RequirementResult(
                    "General Education",
                    false,
                    Set.of("Need " + missingCount + " more GenEd courses.")
            );
        }
    }

    public List<GenedClusteringService.ClusterSolution> checkGeneralEducationRequirements(UUID studentId) {
        // 1. Fetch the student's completed courses
        List<Course> completedCourses = enrollmentService.getCompletedCourses(studentId);

        // 2. Get the department-specific set of non-GenEd codes
        Set<String> nonGenEdCodes = getNonGenEdCourseCodesForMajor();

        // 3. Filter out non-GenEd => the remainder are GenEd courses
        List<Course> genEdCompletedCourses = completedCourses.stream()
                .filter(course -> !nonGenEdCodes.contains(course.getCode()))
                .toList();

        return genedClusteringService.findPossibleClusterCombinations(genEdCompletedCourses, 5);
    }

    protected RequirementResult checkFreeElectiveRequirements(UUID studentId) {
        return null;
    }

    public Set<NeededCluster> getGenedMissing(UUID studentId) {
        return genedClusteringService.findNeededClusters(enrollmentService.getCompletedCourses(studentId));
    }
}
