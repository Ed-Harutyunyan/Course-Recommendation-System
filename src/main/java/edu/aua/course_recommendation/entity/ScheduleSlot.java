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

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false)
    private CourseType courseType;    // e.g. "CORE", "GENED", "TRACK", "PE", "FIRST_AID"

    @Column(name = "offering_id", nullable = false)
    private UUID offeringId;    // the specific CourseOffering ID

    @Column(nullable = false)
    private int credits;        // e.g. 3 or 0

    @Column(nullable = false)
    private String times;       // e.g. "TUE 10:00-11:15, THU 10:00-11:15"
}



