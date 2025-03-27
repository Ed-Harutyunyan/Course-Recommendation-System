package edu.aua.course_recommendation.model;

public enum AcademicStanding {
    FRESHMAN(0, 29),    // 0-29 credits
    SOPHOMORE(30, 59),  // 30-59 credits
    JUNIOR(60, 89),     // 60-89 credits
    SENIOR(90, 999);    // 90+ credits

    private final int minCredits;
    private final int maxCredits;

    AcademicStanding(int minCredits, int maxCredits) {
        this.minCredits = minCredits;
        this.maxCredits = maxCredits;
    }

    public static AcademicStanding getStandingFromCredits(int totalCredits) {
        for (AcademicStanding standing : values()) {
            if (totalCredits >= standing.minCredits && totalCredits <= standing.maxCredits) {
                return standing;
            }
        }
        return SENIOR;
    }
}