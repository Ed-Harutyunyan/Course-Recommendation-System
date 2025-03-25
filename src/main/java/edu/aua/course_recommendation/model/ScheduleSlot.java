package edu.aua.course_recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleSlot {
    private String category;    // e.g. "CORE", "GENED", "TRACK", "PE", "FIRST_AID" (Maybe enum)
    private UUID offeringId;    // the specific CourseOffering ID
    private int credits;        // 3 or 0
    private String times;       // "TUE 10:00-11:15, THU 10:00-11:15"
}


