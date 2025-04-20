package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.model.DegreeAuditScenario;
import edu.aua.course_recommendation.model.DegreeScenarioType;
import edu.aua.course_recommendation.model.Requirement;
import edu.aua.course_recommendation.model.RequirementResult;
import edu.aua.course_recommendation.service.auth.UserService;
import edu.aua.course_recommendation.service.course.CourseService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CSDegreeAuditService extends BaseDegreeAuditService{

    public CSDegreeAuditService(EnrollmentService enrollmentService,
                                GenedClusteringService genedClusteringService,
                                CourseService courseService,
                                UserService userService) {
        super(enrollmentService, genedClusteringService, courseService, userService);
    }

    // TODO: Remove these hard-coded values eventually
    // I imagine the logic can be used across all degrees
    // I thin only these static fields and MAYBE track counts change
    // The core logic should be the same
    private static final int TRACK_REQUIREMENT_COUNT = 5;
    private static final int FREE_ELECTIVE_REQUIREMENT_COUNT = 3;
    private static final String CAPSTONE_REQUIREMENT = "CS296";

    // Static for now, will change it so it is saved in db and update-able
    private static final List<String> CS_CORE = List.of(
            "CS110", // Intro to Computer Science (Fall 1)
            "CS100", // Calculus 1
            "CS111", // Discrete Math

            "CS120", // Intro to OOP (Spring 1)
            "CS101", // Calculus 2
            "CS104", // Linear Algebra

            "CS121", // Data Structures (Fall 2)
            "CS102", // Calculus 3
            "CS130", // Computer Organization

            "CS211", // Intro to Algorithms (Spring 2)
            "CS103", // Real Analysis
            "CS107", // Probability

            "CS112", // Numerical Analysis (Fall 3)
            "CS108", // Statistics

            "CS213", // Optimization (Spring 3)
            "CS140"  // Mechanics
    );


    private final static List<String> MATH_MODELING = List.of(
            "CS105", "CS205", "CS226", "CS221",
            "CS215", "CS217", "CS246", "CS251",
            "CS260", "CS231", "CS261", "CS262",
            "CS310", "DS231", "DS233", "DS330"
    );

    private final static List<String> APPLIED_CS = List.of(
            "CS215", "CS217", "CS246", "CS251",
            "CS260", "CS231", "CS261", "CS262",
            "CS310", "DS231", "DS233", "DS330",
            "CS132", "CS220", "CS222", "CS131",
            "CS221", "CS232", "CS236", "CS245", "CS252"
    );

    // This can just be computed using the above, instead of hardcoding.
    private final static List<String> GENERAL_CS_TRACK = List.of(
            // Any combination of 5 courses from applied and math modeling
            "CS105", "CS205", "CS226", "CS221",
            "CS215", "CS217", "CS246", "CS251",
            "CS260", "CS231", "CS261", "CS262",
            "CS310", "DS231", "DS233", "DS330",
            "CS132", "CS220", "CS222", "CS131",
            "CS232", "CS236", "CS245", "CS252"
    );

    @Override
    public RequirementResult checkProgramCore(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        // Which of the CS core are missing?
        List<String> missing = CS_CORE.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult(
                Requirement.CORE,
                isSatisfied,
                List.copyOf(missing),
                missing.size()
        );
    }

    /**
     * Override the method from the base class to produce
     * scenario-based checks for the CS degree (tracks).
     */
    @Override
    public List<DegreeAuditScenario> checkProgramScenarios(UUID studentId) {
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
                Requirement.TRACK,
                completedCount >= 5,
                List.copyOf(missingCodes),
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
                Requirement.TRACK,
                completedCount >= 5,
                List.copyOf(missingCodes),
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
                Requirement.TRACK,
                completedCount >= 5,
                List.copyOf(missingCodes),
                (int) (TRACK_REQUIREMENT_COUNT - completedCount)
        );
    }

    @Override
    protected List<String> getCoreAndTrackCourseCodes() {
        // Union of CS core + all track sets for CS
        List<String> combined = new ArrayList<>(CS_CORE);
        combined.addAll(MATH_MODELING);
        combined.addAll(APPLIED_CS);
        combined.addAll(GENERAL_CS_TRACK);
        return combined;
    }

    @Override
    protected List<String> getCoreCourseCodes() {
        return new ArrayList<>(CS_CORE);
    }

    // The "chosen" track is the one with the most courses
    // If they are equal we'll just assume the general track
    // TODO: Extend so student can explicitly choose their track
    @Override
    public DegreeScenarioType pickChosenTrack(UUID studentId) {
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
    public List<String> getTrackCourseCodes(DegreeScenarioType trackType) {
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

    @Override
    public String getCapstoneCode() {
        return CAPSTONE_REQUIREMENT;
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
