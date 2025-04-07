package edu.aua.course_recommendation.entity;

import edu.aua.course_recommendation.model.CourseType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSlot {

    @Column(name = "offering_id", nullable = false)
    private UUID offeringId;    // the specific CourseOffering ID

    @Column(name = "course_code", nullable = false)
    private String courseCode;   // e.g. "CS101", "MATH201"

    @Column(nullable = false)
    private int credits;        // e.g. 3 or 0

    @Column(nullable = false)
    private String times;       // e.g. "TUE 10:00-11:15, THU 10:00-11:15"
}



