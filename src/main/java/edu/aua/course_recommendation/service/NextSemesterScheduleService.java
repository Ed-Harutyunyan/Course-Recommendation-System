package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.model.NextSemesterSchedule;
import edu.aua.course_recommendation.model.ScheduleSlot;
import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import edu.aua.course_recommendation.service.course.EnrollmentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class NextSemesterScheduleService {

    private static final int MAX_CREDITS = 15;

    private final EnrollmentService enrollmentService;
    private final BaseDegreeAuditService baseDegreeAuditService;
    private final CourseOfferingService courseOfferingService;
    // Python will go here to fetch the recs

    // TODO: Change so year and semester in inferred from local time and no need to provide it
    public NextSemesterSchedule getNextSemester(UUID studentId, String year, String semester) {
        // 1. Fetch the next semesters offerings
        List<CourseOffering> nextSemesterOfferings = courseOfferingService.
                getCourseOfferingsByYearAndSemester(year, semester);

        // 2. Filter out courses that the student already completed.
        List<String> completedCodes = enrollmentService.getCompletedCourseCodes(studentId);

        List<CourseOffering> available = nextSemesterOfferings.stream()
                .filter(o -> !completedCodes.contains(o.getBaseCourse().getCode()))
                .toList();

        List<ScheduleSlot> slots = new ArrayList<>();

        int currentCredits = 0;

//        // 3. Add zero-credit items if needed
//        currentCredits += addZeroCreditItems(slots, studentId);
//
//        // 4. Add up to 3 core courses
//        currentCredits += addCoreCourses(slots, available, studentId, currentCredits);
//
//        // 5. If GenEd not met, add 1 GenEd course
//        boolean genEdMet = baseDegreeAuditService.checkGened(studentId);
//        if (!genEdMet && currentCredits < MAX_CREDITS) {
//            addGenEdCourse(slots, available, currentCredits);
//        }
//
//        // 6. Potentially add track or free elective if there's still space
//        // addTrackOrElective(slots, available, currentCredits);
//
        return new NextSemesterSchedule(slots);
    }

    /**
     * Add zero-credit items like PE, First Aid, Civil Defense if missing.
     * We'll just add placeholders here, each with 0 credits.
     */
    private int addZeroCreditItems(List<ScheduleSlot> slots, UUID studentId) {
        int totalCredits = 0;
        // Check if the student needs first aid, etc.
        if (needsFirstAid(studentId)) {
            slots.add(new ScheduleSlot("FIRST_AID", null, 0, "N/A"));
        }
        if (needsCivilDefense(studentId)) {
            slots.add(new ScheduleSlot("CIVIL_DEFENSE", null, 0, "N/A"));
        }
        // If <4 PE done, add 1 PE
        long peCount = enrollmentService.getCompletedCourseCodes(studentId).stream()
                .filter(code -> code.startsWith("FND110"))
                .count();
        if (peCount < 4) {
            slots.add(new ScheduleSlot("PHYSICAL_ED", null, 0, "N/A"));
        }
        return totalCredits; // 0
    }

    // These 2 use hardcoded values.
    private boolean needsCivilDefense(UUID studentId) {
        return enrollmentService.getCompletedCourseCodes(studentId).contains("FND153");
    }

    private boolean needsFirstAid(UUID studentId) {
        return enrollmentService.getCompletedCourseCodes(studentId).contains("FND152");
    }

}
