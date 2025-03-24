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

    // TODO: Remove these hard-coded values eventually
    // I imagine the logic can be used across all degrees
    // I thin only these static fields and MAYBE track counts change
    // The core logic should be the same
    private static final int TRACK_REQUIREMENT_COUNT = 5;
    private static final int FREE_ELECTIVE_REQUIREMENT_COUNT = 3;

    // Static for now, will change it so it is saved in db and update-able
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

    @Override
    protected RequirementResult checkProgramCore(UUID studentId) {
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
        mathScenario.addRequirementResult(checkMathModelingTrackRequirement(studentId));
        mathScenario.canGraduate();
        scenarios.add(mathScenario);

        // 2. Applied CS scenario
        DegreeAuditScenario appliedScenario = new DegreeAuditScenario(DegreeScenarioType.CS_APPLIED_CS, new ArrayList<>(), false);
        appliedScenario.addRequirementResult(checkAppliedTrackRequirement(studentId));
        appliedScenario.canGraduate();
        scenarios.add(appliedScenario);

        // 3. General scenario (if you have one)
        DegreeAuditScenario generalScenario = new DegreeAuditScenario(DegreeScenarioType.CS_GENERAL, new ArrayList<>(), false);
        generalScenario.addRequirementResult(checkGeneralTrackRequirement(studentId));
        generalScenario.canGraduate();
        scenarios.add(generalScenario);

        return scenarios;
    }


    /**
     * Example: math modeling track requirement.
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
    protected Set<String> getCoreAndTrackCourseCodes() {
        // Union of CS core + all track sets for CS
        Set<String> combined = new HashSet<>(CS_CORE);
        combined.addAll(MATH_MODELING);
        combined.addAll(APPLIED_CS);
        combined.addAll(GENERAL_CS_TRACK);
        return combined;
    }

    @Override
    protected Set<String> getCoreCourseCodes() {
        return new HashSet<>(CS_CORE);
    }

    // The "chosen" track is the one with the most courses
    // If they are equal we'll just assume the general track
    // TODO: Extend so student can explicitly choose their track
    @Override
    protected DegreeScenarioType pickChosenTrack(UUID studentId) {
        long mmCount = countMathModelingTrackCompleted(studentId);
        long csCount = countAppliedCSTrackCompleted(studentId);
        long genCount = countGeneralTrackCompleted(studentId);

        if (mmCount >= 5) return DegreeScenarioType.CS_MATH_MODELING;
        if (csCount >= 5) return DegreeScenarioType.CS_APPLIED_CS;
        if (genCount >= 5) return DegreeScenarioType.CS_GENERAL;

        // else pick the track with the highest partial
        long max = Math.max(mmCount, Math.max(csCount, genCount));
        if (max == 0) {
            // default to GENERAL
            return DegreeScenarioType.CS_GENERAL;
        } else if (max == mmCount) {
            return DegreeScenarioType.CS_MATH_MODELING;
        } else if (max == csCount) {
            return DegreeScenarioType.CS_APPLIED_CS;
        } else {
            return DegreeScenarioType.CS_GENERAL;
        }

    }

    @Override
    protected Set<String> getTrackCourseCodes(DegreeScenarioType trackType) {
        if (trackType == DegreeScenarioType.CS_MATH_MODELING) {
            return MATH_MODELING;
        } else if (trackType == DegreeScenarioType.CS_APPLIED_CS) {
            return APPLIED_CS;
        } else {
            // default or CS_GENERAL
            return GENERAL_CS_TRACK;
        }
    }


    @Override
    protected int getRequiredFreeElectiveCount() {
        return FREE_ELECTIVE_REQUIREMENT_COUNT;
    }

    private long countMathModelingTrackCompleted(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);
        return MATH_MODELING.stream().filter(completed::contains).count();
    }

    private long countGeneralTrackCompleted(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);
        return GENERAL_CS_TRACK.stream().filter(completed::contains).count();
    }

    private long countAppliedCSTrackCompleted(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);
        return APPLIED_CS.stream().filter(completed::contains).count();
    }

}
