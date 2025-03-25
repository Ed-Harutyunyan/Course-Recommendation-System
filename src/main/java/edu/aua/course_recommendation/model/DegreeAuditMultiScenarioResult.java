package edu.aua.course_recommendation.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class DegreeAuditMultiScenarioResult {

    private List<RequirementResult> commonRequirements;
    private boolean commonRequirementsSatisfied;

    private List<DegreeAuditScenario> scenarios;

    private boolean canGraduateInAnyScenario;

    public DegreeAuditMultiScenarioResult(List<RequirementResult> commonRequirements, List<DegreeAuditScenario> scenarios) {
        this.commonRequirements = commonRequirements;
        this.scenarios = scenarios;
        this.commonRequirementsSatisfied = commonRequirements.stream().allMatch(RequirementResult::isSatisfied);
        this.canGraduateInAnyScenario = this.commonRequirementsSatisfied &&
                scenarios.stream().anyMatch(DegreeAuditScenario::isSatisfied);
    }
}
