package edu.aua.course_recommendation.util;

import lombok.Getter;

import java.time.LocalDate;
import java.time.Month;

public class AcademicCalendarUtil {

    @Getter
    public enum Semester {
        FALL("1"),
        WINTER("2"),
        SPRING("3"),
        SUMMER("4");

        private final String value;

        Semester(String value) {
            this.value = value;
        }

        public String getName() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    /**
     * Determines the next academic year and semester based on current date.
     * Academic years are in format "202425" for 2024-2025 academic year.
     *
     * @return String[] with [year, semester] where semester is "1" for Fall, "3" for Spring, "4" for Summer
     */
    public static String[] getNextAcademicPeriod() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        Month currentMonth = today.getMonth();
        int dayOfMonth = today.getDayOfMonth();

        // Determine current semester first
        Semester currentSemester;
        int academicYearStart;

        if (currentMonth.getValue() >= Month.JANUARY.getValue() && currentMonth.getValue() <= Month.MAY.getValue()) {
            // January through May: We're in Spring semester
            currentSemester = Semester.SPRING;
            academicYearStart = currentYear - 1; // Academic year started last fall
        } else if (currentMonth.getValue() >= Month.JUNE.getValue() && currentMonth.getValue() <= Month.AUGUST.getValue()) {
            // June through August: We're in Summer semester
            currentSemester = Semester.SUMMER;
            academicYearStart = currentYear - 1; // Academic year started last fall
        } else {
            // September through December: We're in Fall semester
            currentSemester = Semester.FALL;
            academicYearStart = currentYear; // Academic year starts this fall
        }

        // Now determine next semester based on current semester
        Semester nextSemester;
        int nextAcademicYearStart = academicYearStart;

        switch (currentSemester) {
            case SPRING:
                // After Spring comes Summer
                nextSemester = Semester.SUMMER;
                break;
            case SUMMER:
                // After Summer comes Fall (new academic year)
                nextSemester = Semester.FALL;
                nextAcademicYearStart = currentYear; // New academic year starts in fall
                break;
            case FALL:
            default:
                // After Fall comes Spring
                nextSemester = Semester.SPRING;
                break;
        }

        // Special case: In August before Fall registration (Aug 14)
        if (currentMonth == Month.AUGUST && dayOfMonth < 14) {
            // Still show Fall as next semester
            nextSemester = Semester.FALL;
            nextAcademicYearStart = currentYear;
        }

        // Format academic year as "YYYYZZ" where YYYY is start year and ZZ is last two digits of end year
        String year = String.format("%d%02d", nextAcademicYearStart, (nextAcademicYearStart + 1) % 100);

        return new String[]{year, nextSemester.getValue()};
    }

    /**
     * Gets the human-readable name of a semester based on its value
     *
     * @param semesterValue The value ("1", "2", "3", "4")
     * @return The name (Fall, Winter, Spring, Summer)
     */
    public static String getSemesterName(String semesterValue) {
        for (Semester s : Semester.values()) {
            if (s.getValue().equals(semesterValue)) {
                return s.getName();
            }
        }
        return "Unknown";
    }
}