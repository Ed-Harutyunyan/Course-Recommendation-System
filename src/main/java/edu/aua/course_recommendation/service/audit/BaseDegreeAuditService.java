package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.model.DegreeAuditMultiScenarioResult;
import edu.aua.course_recommendation.model.DegreeAuditScenario;
import edu.aua.course_recommendation.model.NeededCluster;
import edu.aua.course_recommendation.model.RequirementResult;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseDegreeAuditService {

    private final static List<String> FOUNDATION_REQUIREMENTS = List.of(
            "FND101", "FND102", // Freshman Seminar 1 & 2
            "FND103", "FND104", // Armenian Language & Literature 1 & 2
            "FND221", "FND222"); // Armenian History 1 & 2

    private final static int REQUIRED_GENED_COUNT = 9;
    private static final String FIRST_AID_CODE = "FND152";
    private static final String CIVIL_DEFENSE_CODE = "FND153";

    protected final EnrollmentService enrollmentService;
    private final GenedClusteringService genedClusteringService;
    private final CourseService courseService;

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
        RequirementResult firstAidCivDef = checkFirstAidAndCivilDefense(studentId);

        List<RequirementResult> commonReqs = List.of(foundation, phed, genEd, firstAidCivDef);

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
                Set.copyOf(missing),
                missing.size()
        );
    }

    protected RequirementResult checkPhysicalEducationRequirementsDetailed(UUID studentId) {

        Set<String> completedPhysedCodes = enrollmentService.getCompletedCourses(studentId).stream()
                .map(Course::getCode)
                .filter(code -> code.startsWith("FND110"))
                .collect(Collectors.toSet());

        Set<String> allPhysed = courseService.getAllPhysedCourses().stream()
                .map(Course::getCode)
                .collect(Collectors.toSet());

        Set<String> physedCompleted = completedPhysedCodes.stream()
                .filter(allPhysed::contains)
                .collect(Collectors.toSet());

        int completedCount = completedPhysedCodes.size();
        boolean isSatisfied = (completedCount >= 4);
        return new RequirementResult(
                "Physical Education",
                isSatisfied,
                physedCompleted,
                completedCount
        );
    }

    public boolean checkGened(UUID studentId) {
        return genedClusteringService.isGenEdRequirementMet(enrollmentService.getCompletedCourses(studentId));
    }

    /*
     * Check for general education requirements
     * Since this requirement can be completed in different ways
     * Not sure if this should return one possible way or all possible ways (probably all)
     *
     * For each degree a course is gen-ed if its not core or track elective
     */
    public RequirementResult checkGeneralEducationRequirementsDetailed(UUID studentId) {
        // 1. Fetch the student's completed courses
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        // 2. Get the department-specific set of non-GenEd codes
        Set<String> nonGenEdCodes = getNonGenEdCourseCodesForMajor();

        Set<String> allCourses = courseService.getAllCourses().stream().map(Course::getCode).collect(Collectors.toSet());
        Set<String> allGenEdCodes = allCourses.stream()
                .filter(code -> !nonGenEdCodes.contains(code))
                .collect(Collectors.toSet());

        // 4. Which GenEd courses the student has completed
        Set<String> genEdCompleted = completedCodes.stream()
                .filter(allGenEdCodes::contains)
                .collect(Collectors.toSet());

        int needed = REQUIRED_GENED_COUNT - genEdCompleted.size();

        boolean isSatisfied = (needed <= 0);

        Set<String> missingCodes = new HashSet<>();
        if (!isSatisfied) {
            missingCodes = allGenEdCodes.stream()
                    .filter(code -> !genEdCompleted.contains(code))
                    .collect(Collectors.toSet());
        }

        return new RequirementResult(
                "General Education",
                (needed <= 0),
                missingCodes,
                needed
        );
    }

    protected RequirementResult checkFirstAidAndCivilDefense(UUID studentId) {
        // 1. Student’s completed codes
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        // 2. Check if the codes "FND152" and "FND153" are in there
        boolean firstAidDone = completedCodes.contains(FIRST_AID_CODE);
        boolean civilDefenseDone = completedCodes.contains(CIVIL_DEFENSE_CODE);

        // 3. If both are done => isSatisfied = true
        boolean isSatisfied = (firstAidDone && civilDefenseDone);

        // 4. For the missing codes, we store them in a set
        Set<String> missing = new HashSet<>();
        if (!firstAidDone) missing.add(FIRST_AID_CODE);
        if (!civilDefenseDone) missing.add(CIVIL_DEFENSE_CODE);

        // 5. Possibly you store how many are missing in the 'count' field
        return new RequirementResult(
                "First Aid & Civil Defense",
                isSatisfied,
                missing,
                missing.size()
        );
    }

    protected RequirementResult checkFreeElectiveRequirements(UUID studentId) {
        return null;
    }

    public Set<NeededCluster> getGenedMissing(UUID studentId) {
        return genedClusteringService.findNeededClusters(enrollmentService.getCompletedCourses(studentId));
    }
}
