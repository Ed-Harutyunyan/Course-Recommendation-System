package edu.aua.course_recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor @RequiredArgsConstructor
public class NeededCluster {
    private int theme;
    private int missingLower;
    private int missingUpper;
    private int missingTotal;
}

