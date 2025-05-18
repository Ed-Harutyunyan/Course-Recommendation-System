package edu.aua.course_recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter @Setter
public class RequirementResult {
    private Requirement requirementName;
    private boolean isSatisfied;
    private List<String> possibleCourseCodes;
    private List<String> completedCourseCodes;
    private int howManyLeft;
}
