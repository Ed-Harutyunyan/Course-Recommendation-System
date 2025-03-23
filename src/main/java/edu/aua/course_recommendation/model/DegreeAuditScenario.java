package edu.aua.course_recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter @Setter
public class DegreeAuditScenario {

    // This should be changed to include all degrees
    private DegreeScenarioType degreeScenarioType; // e.g MATH_MODELING, APPLIED_CS, GENERAL

    private List<RequirementResult> scenarioRequirements = new ArrayList<>();

    private boolean isSatisfied;

    public void canGraduate() {
        isSatisfied = scenarioRequirements.stream().allMatch(RequirementResult::isSatisfied);
    }

    public void addRequirementResult(RequirementResult requirementResult) {
        scenarioRequirements.add(requirementResult);
    }
}
