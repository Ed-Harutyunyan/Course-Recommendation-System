package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.model.DegreeAuditScenario;
import edu.aua.course_recommendation.model.DegreeScenarioType;
import edu.aua.course_recommendation.model.RequirementResult;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CSDegreeAuditService extends BaseDegreeAuditService{

    private final GenedClusteringService genedClusteringService;

    public CSDegreeAuditService(EnrollmentService enrollmentService, GenedClusteringService genedClusteringService) {
        super(enrollmentService, genedClusteringService);
        this.genedClusteringService = genedClusteringService;
    }

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
        // Possibly some "General track" logic if it differs
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
                Set.copyOf(missing)
        );
    }

    /**
     * Example: math modeling track requirement.
     * e.g., Must have "CS105","CS205","CS226" (just an example).
     */
    private RequirementResult checkMathModelingTrackRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        // TODO: Change to see if it contains specific TAGS
        List<String> mathRequired = List.of("CS105","CS205","CS226");
        List<String> missing = mathRequired.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult("Math Modeling Track", isSatisfied, Set.copyOf(missing));
    }

    /**
     * Example: applied track requirement.
     * e.g., Must have "CS132","CS220","CS222" or some other logic
     */
    private RequirementResult checkAppliedTrackRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        // TODO: Change hardcoded to courses with specific TAGS
        List<String> appliedRequired = List.of("CS132","CS220","CS222");
        List<String> missing = appliedRequired.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult("Applied CS Track", isSatisfied, Set.copyOf(missing));
    }

    private RequirementResult checkGeneralTrackRequirements(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        // TODO: Change hardcoded to courses with specific TAGS
        List<String> generalRequired = List.of("CS105","CS205","CS226");
        List<String> missing = generalRequired.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult("General Track", isSatisfied, Set.copyOf(missing));
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
