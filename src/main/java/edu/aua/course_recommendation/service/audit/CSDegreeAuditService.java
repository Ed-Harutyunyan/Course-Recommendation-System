package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.service.course.EnrollmentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CSDegreeAuditService extends BaseDegreeAuditService{

    public CSDegreeAuditService(EnrollmentService enrollmentService) {
        super(enrollmentService);
    }

    private static final List<String> CS_CORE_REQUIREMENTS = List.of(
            "CS100", "CS101", "CS102", "CS103", "CS104",
            "CS111", "CS107", "CS108", "CS110", "CS120",
            "CS121", "CS211", "CS112", "CS213", "CS130",
            "ENGS121", "CS296"
    );

    // VERY INCOMPLETE
    @Override
    protected List<String> checkProgramRequirements(UUID studentId) {
        // Add other CS-specific checks here

        return checkCsCore(studentId);
    }

    private List<String> checkCsCore(UUID studentId) {
        List<String> completedCourses = enrollmentService.getCompletedCourseCodes(studentId);
        return CS_CORE_REQUIREMENTS.stream()
                .filter(required -> !completedCourses.contains(required))
                .toList();

    }
}
