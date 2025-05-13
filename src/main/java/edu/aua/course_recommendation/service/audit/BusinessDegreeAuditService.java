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
import java.util.stream.Stream;

@Service
public class BusinessDegreeAuditService extends BaseDegreeAuditService {

    // ——————————————————————————————————————————————————————
    // Core → fundamentals, 12 fixed, 2 “one-of” groups, capstone
    // ——————————————————————————————————————————————————————
    private static final List<String> CORE_FUNDAMENTALS = List.of("BUS109", "BUS110", "BUS177");
    private static final List<String> CORE_REQUIREMENTS = List.of(
            "BUS101","BUS105","BUS145","BUS146",
            "BUS160","BUS209","BUS211","BUS230",
            "BUS280","BUS295","ECON121","ECON122"
    );
    private static final Set<String> CORE_ONE_OF_1 = Set.of("BUS281","BUS286");
    private static final Set<String> CORE_ONE_OF_2 = Set.of("BUS210","BUS265","ECON225");
    private static final int FREE_ELECTIVE_REQUIREMENT_COUNT = 3;
    private static final String CAPSTONE = "BUS299";

    // ——————————————————————————————————————————————————————
    // Accounting track (5 total: 1 from CORE_ONE_OF_1 + 3 required + 1 elective)
    // ——————————————————————————————————————————————————————
    private static final List<String> ACCOUNTING_REQUIRED = List.of(
            "BUS286","BUS245","BUS247","BUS248"
    );
    private static final List<String> ACCOUNTING_ELECTIVES = List.of(
            "BUS226","BUS232","BUS239","BUS250","BUS253","BUS254","BUS257"
    );
    private static final int ACCOUNTING_ELECTIVE_PICKS = 2;

    // ——————————————————————————————————————————————————————
    // Economics track
    // ——————————————————————————————————————————————————————
    private static final List<String> ECONOMICS_REQUIRED = List.of(
            "ECON225","ECON221","ECON222","ECON224"
    );
    private static final List<String> ECONOMICS_ELECTIVES = List.of(
            "BUS226","BUS227","BUS232","BUS233","BUS234","BUS239",
            "ECON120","ECON223","ECON228","ECON229"
    );
    private static final int ECONOMICS_ELECTIVE_PICKS = 2;

    // ——————————————————————————————————————————————————————
    // Marketing track
    // ——————————————————————————————————————————————————————
    private static final List<String> MARKETING_REQUIRED = List.of(
            "BUS265","BUS262","BUS275","BUS276"
    );
    private static final List<String> MARKETING_ELECTIVES = List.of(
            "BUS261","BUS263","BUS266","BUS271","BUS274","BUS278","BUS279"
    );
    private static final int MARKETING_ELECTIVE_PICKS = 2;

    private static final int GENERAL_BUSINESS_TOTAL = 5;

    // ——————————————————————————————————————————————————————
    // Business Electives
    // Course here can act either as General Business track courses or free electives.
    // ——————————————————————————————————————————————————————
    private static final List<String> BUSINESS_ELECTIVES = List.of(
            "BUS114","BUS201","BUS207","BUS218",
            "BUS282","BUS285","BUS287","BUS288",
            "BUS290","BUS292","BUS298","ECON201"
    );

    public BusinessDegreeAuditService(
            EnrollmentService enrollmentService,
            GenedClusteringService genedClusteringService,
            CourseService courseService,
            UserService userService
    ) {
        super(enrollmentService, genedClusteringService, courseService, userService);
    }

    // ——————————————————————————————————————————————————————
    // 1) CORE Requirements
    // ——————————————————————————————————————————————————————
    @Override
    public RequirementResult checkProgramCore(UUID studentId) {
        List<String> done = enrollmentService.getCompletedCourseCodes(studentId);

        List<String> missFund = CORE_FUNDAMENTALS.stream()
                .filter(c -> !done.contains(c))
                .toList();

        List<String> missCore = CORE_REQUIREMENTS.stream()
                .filter(c -> !done.contains(c))
                .toList();

        boolean one1Ok = CORE_ONE_OF_1.stream().anyMatch(done::contains);
        boolean one2Ok = CORE_ONE_OF_2.stream().anyMatch(done::contains);

        List<String> missOne1 = one1Ok ? List.of() : new ArrayList<>(CORE_ONE_OF_1);
        List<String> missOne2 = one2Ok ? List.of() : new ArrayList<>(CORE_ONE_OF_2);

        List<String> allMissing = Stream.of(missFund, missCore, missOne1, missOne2)
                .flatMap(Collection::stream)
                .distinct()
                .toList();

        boolean satisfied = allMissing.isEmpty();
        return new RequirementResult(
                Requirement.CORE,
                satisfied,
                allMissing,
                allMissing.size()
        );
    }

    // ——————————————————————————————————————————————————————
    // 2) TRACK Scenarios
    // ——————————————————————————————————————————————————————
    @Override
    public List<DegreeAuditScenario> checkProgramScenarios(UUID studentId) {
        List<DegreeAuditScenario> scenarios = new ArrayList<>();

        // Accounting
        DegreeAuditScenario acct = new DegreeAuditScenario(
                DegreeScenarioType.BUS_ACCOUNTING, new ArrayList<>(), false);
        acct.addRequirementResult(checkAccountingTrack(studentId));
        acct.canGraduate();
        scenarios.add(acct);

        // Economics
        DegreeAuditScenario econ = new DegreeAuditScenario(
                DegreeScenarioType.BUS_ECONOMICS, new ArrayList<>(), false);
        econ.addRequirementResult(checkEconomicsTrack(studentId));
        econ.canGraduate();
        scenarios.add(econ);

        // Marketing
        DegreeAuditScenario mkt = new DegreeAuditScenario(
                DegreeScenarioType.BUS_MARKETING, new ArrayList<>(), false);
        mkt.addRequirementResult(checkMarketingTrack(studentId));
        mkt.canGraduate();
        scenarios.add(mkt);

        // General
        DegreeAuditScenario gen = new DegreeAuditScenario(
                DegreeScenarioType.BUS_GENERAL, new ArrayList<>(), false);
        gen.addRequirementResult(checkGeneralBusinessTrack(studentId));
        gen.canGraduate();
        scenarios.add(gen);

        return scenarios;
    }

    private RequirementResult checkAccountingTrack(UUID studentId) {
        List<String> done = enrollmentService.getCompletedCourseCodes(studentId);

        // required
        List<String> missReq = ACCOUNTING_REQUIRED.stream()
                .filter(c -> !done.contains(c))
                .toList();

        // electives taken
        long takenElect = ACCOUNTING_ELECTIVES.stream()
                .filter(done::contains)
                .count();
        int neededElect = ACCOUNTING_ELECTIVE_PICKS - (int)takenElect;

        // which electives are still possible
        List<String> missElect = ACCOUNTING_ELECTIVES.stream()
                .filter(c -> !done.contains(c))
                .toList();

        boolean satisfied = missReq.isEmpty() && neededElect <= 0;
        List<String> missing = Stream.concat(missReq.stream(), missElect.stream())
                .distinct()
                .toList();

        return new RequirementResult(
                Requirement.TRACK,
                satisfied,
                missing,
                missReq.size() + Math.max(0, neededElect)
        );
    }

    private RequirementResult checkEconomicsTrack(UUID studentId) {
        List<String> done = enrollmentService.getCompletedCourseCodes(studentId);

        List<String> missReq = ECONOMICS_REQUIRED.stream()
                .filter(c -> !done.contains(c))
                .toList();

        long takenElect = ECONOMICS_ELECTIVES.stream()
                .filter(done::contains)
                .count();
        int neededElect = ECONOMICS_ELECTIVE_PICKS - (int)takenElect;

        List<String> missElect = ECONOMICS_ELECTIVES.stream()
                .filter(c -> !done.contains(c))
                .toList();

        boolean satisfied = missReq.isEmpty() && neededElect <= 0;
        List<String> missing = Stream.concat(missReq.stream(), missElect.stream())
                .distinct()
                .toList();

        return new RequirementResult(
                Requirement.TRACK,
                satisfied,
                missing,
                missReq.size() + Math.max(0, neededElect)
        );
    }

    private RequirementResult checkMarketingTrack(UUID studentId) {
        List<String> done = enrollmentService.getCompletedCourseCodes(studentId);

        List<String> missReq = MARKETING_REQUIRED.stream()
                .filter(c -> !done.contains(c))
                .toList();

        long takenElect = MARKETING_ELECTIVES.stream()
                .filter(done::contains)
                .count();
        int neededElect = MARKETING_ELECTIVE_PICKS - (int)takenElect;

        List<String> missElect = MARKETING_ELECTIVES.stream()
                .filter(c -> !done.contains(c))
                .toList();

        boolean satisfied = missReq.isEmpty() && neededElect <= 0;
        List<String> missing = Stream.concat(missReq.stream(), missElect.stream())
                .distinct()
                .toList();

        return new RequirementResult(
                Requirement.TRACK,
                satisfied,
                missing,
                missReq.size() + Math.max(0, neededElect)
        );
    }

    private RequirementResult checkGeneralBusinessTrack(UUID studentId) {
        List<String> done = enrollmentService.getCompletedCourseCodes(studentId);

        // Pool = all required/electives from every track + business electives
        List<String> pool = Stream.of(
                        ACCOUNTING_REQUIRED, ACCOUNTING_ELECTIVES,
                        ECONOMICS_REQUIRED, ECONOMICS_ELECTIVES,
                        MARKETING_REQUIRED, MARKETING_ELECTIVES,
                        BUSINESS_ELECTIVES
                )
                .flatMap(Collection::stream)
                .distinct()
                .toList();

        long taken = pool.stream().filter(done::contains).count();
        int needed = GENERAL_BUSINESS_TOTAL - (int)taken;

        List<String> missing = pool.stream()
                .filter(c -> !done.contains(c))
                .toList();

        boolean satisfied = needed <= 0;
        return new RequirementResult(
                Requirement.TRACK,
                satisfied,
                missing,
                needed
        );
    }

    // ——————————————————————————————————————————————————————
    // 3) Other abstract methods
    // ——————————————————————————————————————————————————————

    @Override
    protected List<String> getCoreAndTrackCourseCodes() {
        return Stream.of(
                        CORE_FUNDAMENTALS,
                        CORE_REQUIREMENTS,
                        CORE_ONE_OF_1,
                        CORE_ONE_OF_2,
                        ACCOUNTING_REQUIRED, ACCOUNTING_ELECTIVES,
                        ECONOMICS_REQUIRED, ECONOMICS_ELECTIVES,
                        MARKETING_REQUIRED, MARKETING_ELECTIVES
                )
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    @Override
    protected List<String> getCoreCourseCodes() {
        return Stream.of(
                        CORE_FUNDAMENTALS,
                        CORE_REQUIREMENTS,
                        new ArrayList<>(CORE_ONE_OF_1),
                        new ArrayList<>(CORE_ONE_OF_2)
                )
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    @Override
    public List<String> getTrackCourseCodes(DegreeScenarioType trackType) {
        return switch (trackType) {
            case BUS_ACCOUNTING -> Stream
                    .concat(ACCOUNTING_REQUIRED.stream(), ACCOUNTING_ELECTIVES.stream())
                    .distinct()
                    .toList();

            case BUS_ECONOMICS -> Stream
                    .concat(ECONOMICS_REQUIRED.stream(), ECONOMICS_ELECTIVES.stream())
                    .distinct()
                    .toList();

            case BUS_MARKETING -> Stream
                    .concat(MARKETING_REQUIRED.stream(), MARKETING_ELECTIVES.stream())
                    .distinct()
                    .toList();

            case BUS_GENERAL -> Stream.of(
                            ACCOUNTING_REQUIRED, ACCOUNTING_ELECTIVES,
                            ECONOMICS_REQUIRED, ECONOMICS_ELECTIVES,
                            MARKETING_REQUIRED, MARKETING_ELECTIVES,
                            BUSINESS_ELECTIVES
                    )
                    .flatMap(Collection::stream)
                    .distinct()
                    .toList();

            default -> List.of();
        };
    }


    @Override
    public DegreeScenarioType pickChosenTrack(UUID studentId) {
        List<String> done = enrollmentService.getCompletedCourseCodes(studentId);

        // Count completed courses for specific tracks (Required + Electives)
        long a = Stream
                .concat(ACCOUNTING_REQUIRED.stream(), ACCOUNTING_ELECTIVES.stream())
                .filter(done::contains)
                .distinct()
                .count();
        long e = Stream
                .concat(ECONOMICS_REQUIRED.stream(), ECONOMICS_ELECTIVES.stream())
                .filter(done::contains)
                .distinct()
                .count();
        long m = Stream
                .concat(MARKETING_REQUIRED.stream(), MARKETING_ELECTIVES.stream())
                .filter(done::contains)
                .distinct()
                .count();

        // Count completed courses applicable to the General Business track pool
        // Pool = all required/electives from every track + business electives
        List<String> generalPool = Stream.of(
                        ACCOUNTING_REQUIRED, ACCOUNTING_ELECTIVES,
                        ECONOMICS_REQUIRED, ECONOMICS_ELECTIVES,
                        MARKETING_REQUIRED, MARKETING_ELECTIVES,
                        BUSINESS_ELECTIVES
                )
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        long g = generalPool.stream().filter(done::contains).count();


        // Pick the track with the highest count, prioritizing A > E > M > G in ties
        if (a >= e && a >= m && a >= g) {
            return DegreeScenarioType.BUS_ACCOUNTING;
        } else if (e >= a && e >= m && e >= g) {
            return DegreeScenarioType.BUS_ECONOMICS;
        } else if (m >= a && m >= e && m >= g) {
            return DegreeScenarioType.BUS_MARKETING;
        } else {
            // If none of the specific tracks had the highest count (considering priority),
            // then General Business must have the highest count or is chosen due to ties.
            return DegreeScenarioType.BUS_GENERAL;
        }
    }

    @Override
    protected int getRequiredFreeElectiveCount() {
        return FREE_ELECTIVE_REQUIREMENT_COUNT;
    }

    @Override
    public String getCapstoneCode() {
        return CAPSTONE;
    }
}
