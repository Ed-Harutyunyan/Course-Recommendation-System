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

    private static final int TRACK_REQUIREMENT_COUNT = 5;
    private static final int FREE_ELECTIVE_REQUIREMENT_COUNT = 3;
    private static final String CAPSTONE_REQUIREMENT = "CS296";

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

    private final static List<String> GENERAL_CS_TRACK = List.of(
            // Any combination of 5 courses from applied and math modeling
            "CS105", "CS205", "CS226", "CS221",
            "CS215", "CS217", "CS246", "CS251",
            "CS260", "CS231", "CS261", "CS262",
            "CS310", "DS231", "DS233", "DS330",
            "CS132", "CS220", "CS222", "CS131",
            "CS232", "CS236", "CS245", "CS252", "CS331"
    );

    @Override
    public RequirementResult checkProgramCore(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        List<String> missing = CS_CORE.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        List<String> completedCore = CS_CORE.stream()
                .filter(completed::contains)
                .toList();

        boolean isSatisfied = missing.isEmpty();
        return new RequirementResult(
                Requirement.CORE,
                isSatisfied,
                List.copyOf(missing),
                List.copyOf(completedCore),
                missing.size()
        );
    }

    @Override
    public List<DegreeAuditScenario> checkProgramScenarios(UUID studentId) {

        List<DegreeAuditScenario> scenarios = new ArrayList<>();

        DegreeAuditScenario mathScenario = new DegreeAuditScenario(DegreeScenarioType.CS_MATH_MODELING, new ArrayList<>(), false);
        mathScenario.addRequirementResult(checkMathModelingTrackRequirement(studentId));
        mathScenario.canGraduate();
        scenarios.add(mathScenario);

        DegreeAuditScenario appliedScenario = new DegreeAuditScenario(DegreeScenarioType.CS_APPLIED_CS, new ArrayList<>(), false);
        appliedScenario.addRequirementResult(checkAppliedTrackRequirement(studentId));
        appliedScenario.canGraduate();
        scenarios.add(appliedScenario);

        DegreeAuditScenario generalScenario = new DegreeAuditScenario(DegreeScenarioType.CS_GENERAL, new ArrayList<>(), false);
        generalScenario.addRequirementResult(checkGeneralTrackRequirement(studentId));
        generalScenario.canGraduate();
        scenarios.add(generalScenario);

        return clearPossibleCodesIfAnyTrackSatisfied(scenarios);
    }

    private RequirementResult checkMathModelingTrackRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        long completedCount = MATH_MODELING.stream()
                .filter(completed::contains)
                .count();

        List<String> missingCodes = MATH_MODELING.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        List<String> completedCodes = MATH_MODELING.stream()
                .filter(completed::contains)
                .toList();

        return new RequirementResult(
                Requirement.TRACK,
                completedCount >= 5,
                completedCount >= 5 ? List.of() : List.copyOf(missingCodes),
                completedCodes,
                (int) (TRACK_REQUIREMENT_COUNT - completedCount)
        );
    }

    private RequirementResult checkAppliedTrackRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        long completedCount = APPLIED_CS.stream()
                .filter(completed::contains)
                .count();

        List<String> missingCodes = APPLIED_CS.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        List<String> completedCodes = APPLIED_CS.stream()
                .filter(completed::contains)
                .toList();

        return new RequirementResult(
                Requirement.TRACK,
                completedCount >= 5,
                completedCount >= 5 ? List.of() : List.copyOf(missingCodes),
                completedCodes,
                (int) (TRACK_REQUIREMENT_COUNT - completedCount)
        );
    }

    private RequirementResult checkGeneralTrackRequirement(UUID studentId) {
        List<String> completed = enrollmentService.getCompletedCourseCodes(studentId);

        long completedCount = GENERAL_CS_TRACK.stream()
                .filter(completed::contains)
                .count();

        List<String> missingCodes = GENERAL_CS_TRACK.stream()
                .filter(req -> !completed.contains(req))
                .toList();

        List<String> completedCodes = GENERAL_CS_TRACK.stream()
                .filter(completed::contains)
                .toList();

        return new RequirementResult(
                Requirement.TRACK,
                completedCount >= 5,
                completedCount >= 5 ? List.of() : List.copyOf(missingCodes),
                completedCodes,
                (int) (TRACK_REQUIREMENT_COUNT - completedCount)
        );
    }

    @Override
    protected List<String> getCoreAndTrackCourseCodes() {
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

    // TODO: Extend so student can explicitly choose their track
    @Override
    public DegreeScenarioType pickChosenTrack(UUID studentId) {
        long mmCount = countMathModelingTrackCompleted(studentId);
        long csCount = countAppliedCSTrackCompleted(studentId);
        long genCount = countGeneralTrackCompleted(studentId);

        if (mmCount >= 5) return DegreeScenarioType.CS_MATH_MODELING;
        if (csCount >= 5) return DegreeScenarioType.CS_APPLIED_CS;
        if (genCount >= 5) return DegreeScenarioType.CS_GENERAL;

        long max = Math.max(mmCount, Math.max(csCount, genCount));
        if (max == 0) {
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
