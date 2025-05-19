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
    private UUID offeringId;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false)
    private String times;
}



