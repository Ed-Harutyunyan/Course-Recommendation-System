package edu.aua.course_recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter @Setter
public class RequirementResult {
    private String requirementName; // TODO: This should be changed to ENUM
    private boolean isSatisfied;
    private List<String> possibleCourseCodes;
    private int howManyLeft;
}
