package edu.aua.course_recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@Getter @Setter
public class RequirementResult {

    private String requirementName; // This should be changed to ENUM
    private boolean isSatisfied;
    private Set<String> possibleCourseCodes;
    private int howManyLeft;


}
