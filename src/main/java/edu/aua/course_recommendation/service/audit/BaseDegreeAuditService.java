package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class BaseDegreeAuditService {

    private final static List<String> FOUNDATION_REQUIREMENTS = List.of(
            "FND101", "FND102", // Freshman Seminar 1 & 2
            "FND103", "FND104", // Armenian Language & Literature 1 & 2
            "FND221", "FND222"); // Armenian History 1 & 2

    protected final EnrollmentService enrollmentService;

    // This should be overridden by each of the specific degree audit services
    // It will be used below in auditStudentDegree to determine if the student has met the requirements for the specific degree
    protected abstract List<String> checkProgramRequirements(UUID studentId);

    // For now this simply returns list of messages for missing requirements
    // needs to be updated
    public List<String> auditStudentDegree(UUID studentId) {

        // Common requirements for all degrees
        List<String> missing = new ArrayList<>(checkFoundationRequirements(studentId));

        int phedMissing = checkPhysicalEducationRequirements(studentId);
        if (phedMissing > 0) {
            missing.add("Need " + phedMissing + " more semesters of Physical Education.");
        }

        // Program-specific requirements
        missing.addAll(checkProgramRequirements(studentId));

        return missing;
    }

    /* Checks for foundation requirements based on the hardcoded list above
     * Returns missing courses codes
     * Empty list means all foundation requirements are fulfilled
     */
    protected List<String> checkFoundationRequirements(UUID studentId) {
        List<String> completedCourses = enrollmentService.getCompletedCourseCodes(studentId);
        return FOUNDATION_REQUIREMENTS.stream()
                .filter(required -> !completedCourses.contains(required))
                .toList();
    }

    /* Check for physical education requirements
     * You need 4 of any course that begins with "FND110"
     * Returns how many more physical educations are needed
     * 0 means fulfilled the requirement
     */
    protected Integer checkPhysicalEducationRequirements(UUID studentId) {
        long completedCount = enrollmentService.getCompletedCourseCodes(studentId).stream()
                .filter(course -> course.startsWith("FND110"))
                .count();
        return Math.max(0, 4 - (int) completedCount);
    }

    /*
     * Check for general education requirements
     * Since this requirement can be completed in different ways
     * Not sure if this should return one possible way or all possible ways (probably all)
     */
    protected List<String> checkGeneralEducationRequirements(UUID studentId) {
        return null;
    }

}
