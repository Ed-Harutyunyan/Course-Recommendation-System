package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.model.DegreeAuditScenario;
import edu.aua.course_recommendation.model.DegreeScenarioType;
import edu.aua.course_recommendation.model.RequirementResult;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CSDegreeAuditService extends BaseDegreeAuditService{

    public CSDegreeAuditService(EnrollmentService enrollmentService,
                                GenedClusteringService genedClusteringService,
                                CourseService courseService) {
        super(enrollmentService, genedClusteringService, courseService);
    }

    private static final int TRACK_REQUIREMENT_COUNT = 5;

    // Static for now, will change it so it is saved in db and updateable
    private static final List<String> CS_CORE = List.of(
            "CS100", "CS101", "CS102", "CS103", "CS104",
            "CS111", "CS107", "CS108", "CS110", "CS120",
            "CS121", "CS211", "CS112", "CS213", "CS130",
            "ENGS121", "CS296"
    );

    private final static Set<String> MATH_MODELING = Set.of(
            "CS105", "CS205", "CS226", "CS221",
            "CS215", "CS217", "CS246", "CS251",
            "CS260", "CS231", "CS261", "CS262",
            "CS310", "DS231", "DS233", "DS330"
    );

    private final static Set<String> APPLIED_CS = Set.of(
            "CS215", "CS217", "CS246", "CS251",
            "CS260", "CS231", "CS261", "CS262",
            "CS310", "DS231", "DS233", "DS330",
            "CS132", "CS220", "CS222", "CS131",
            "CS221", "CS232", "CS236", "CS245", "CS252"
    );

    // This can just be computed using the above, instead of hardcoding.
    private final static Set<String> GENERAL_CS_TRACK = Set.of(
            // Any combination of 5 courses from applied and math modeling
            "CS105", "CS205", "CS226", "CS221",
            "CS215", "CS217", "CS246", "CS251",
            "CS260", "CS231", "CS261", "CS262",
            "CS310", "DS231", "DS233", "DS330",
            "CS132", "CS220", "CS222", "CS131",
            "CS232", "CS236", "CS245", "CS252"
    );

    /**
     * Override the method from the base class to produce
     * scenario-based checks for the CS degree (tracks).
     */
    @Override
    protected List<DegreeAuditScenario> checkProgramScenarios(UUID studentId) {
        // Suppose we have multiple tracks: MATH_MODELING, APPLIED_CS, GENERAL
        // We'll create one scenario object per track
        List<DegreeAuditScenario> scenarios = new ArrayList<>();

        // 1. Math Modeling scenario
        DegreeAuditScenario mathScenario = new DegreeAuditScenario(DegreeScenarioType.CS_MATH_MODELING, new ArrayList<>(), false);
        mathScenario.addRequirementResult(checkCsCoreRequirement(studentId));
        mathScenario.addRequirementResult(checkMathModelingTrackRequirement(studentId));
        mathScenario.canGraduate();
        scenarios.add(mathScenario);

        // 2. Applied CS scenario
        DegreeAuditScenario appliedScenario = new DegreeAuditScenario(DegreeScenarioType.CS_APPLIED_CS, new ArrayList<>(), false);
        appliedScenario.addRequirementResult(checkCsCoreRequirement(studentId));
        appliedScenario.addRequirementResult(checkAppliedTrackRequirement(studentId));
        appliedScenario.canGraduate();
        scenarios.add(appliedScenario);

        // 3. General scenario (if you have one)
        DegreeAuditScenario generalScenario = new DegreeAuditScenario(DegreeScenarioType.CS_GENERAL, new ArrayList<>(), false);
        generalScenario.addRequirementResult(checkCsCoreRequirement(studentId));
        generalScenario.addRequirementResult(checkGeneralTrackRequirement(studentId));
        generalScenario.canGraduate();
        scenarios.add(generalScenario);

        return scenarios;
    }

    /**
     * Check if the student completed all CS core requirements (shared across all CS tracks).
     */
    private RequirementResult checkCsCoreRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        // Which of the CS core are missing?
        List<String> missing = CS_CORE.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult(
                "CS Core Requirements",
                isSatisfied,
                Set.copyOf(missing),
                missing.size()
        );
    }

    /**
     * Example: math modeling track requirement.
     * e.g., Must have "CS105","CS205","CS226" (just an example).
     */
    private RequirementResult checkMathModelingTrackRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        // 1. How many from MATH_MODELING are already completed?
        long completedCount = MATH_MODELING.stream()
                .filter(completed::contains)
                .count();

        List<String> missingCodes = MATH_MODELING.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        return new RequirementResult(
                "Math Modeling Track",
                completedCount >= 5,
                Set.copyOf(missingCodes),
                (int) (TRACK_REQUIREMENT_COUNT - completedCount)
        );
    }

    /**
     * Example: applied track requirement.
     * e.g., Must have "CS132","CS220","CS222" or some other logic
     */
    private RequirementResult checkAppliedTrackRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        // 1. How many from APPLIED_CS are already completed?
        long completedCount = APPLIED_CS.stream()
                .filter(completed::contains)
                .count();

        List<String> missingCodes = APPLIED_CS.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        return new RequirementResult(
                "Applied CS Track",
                completedCount >= 5,
                Set.copyOf(missingCodes),
                (int) (TRACK_REQUIREMENT_COUNT - completedCount)
        );
    }

    private RequirementResult checkGeneralTrackRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        // 1. How many from GENERAL_CS_TRACK are already completed?
        long completedCount = GENERAL_CS_TRACK.stream()
                .filter(completed::contains)
                .count();

        List<String> missingCodes = GENERAL_CS_TRACK.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        return new RequirementResult(
                "General CS Track",
                completedCount >= 5,
                Set.copyOf(missingCodes),
                (int) (TRACK_REQUIREMENT_COUNT - completedCount)
        );
    }

    @Override
    protected Set<String> getNonGenEdCourseCodesForMajor() {
        // Union of CS core + all track sets for CS
        Set<String> combined = new HashSet<>(CS_CORE);
        combined.addAll(MATH_MODELING);
        combined.addAll(APPLIED_CS);
        combined.addAll(GENERAL_CS_TRACK);
        return combined;
    }

}
