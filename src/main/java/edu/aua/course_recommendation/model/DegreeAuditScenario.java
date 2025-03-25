package edu.aua.course_recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter @Setter
public class DegreeAuditScenario {

    private DegreeScenarioType degreeScenarioType;

    private List<RequirementResult> scenarioRequirements;

    private boolean isSatisfied;

    public void canGraduate() {
        isSatisfied = scenarioRequirements.stream().allMatch(RequirementResult::isSatisfied);
    }

    public void addRequirementResult(RequirementResult requirementResult) {
        scenarioRequirements.add(requirementResult);
    }
}
