package edu.aua.course_recommendation.model;

import edu.aua.course_recommendation.entity.ScheduleSlot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NextSemesterSchedule {
    private List<ScheduleSlot> slots;
}



