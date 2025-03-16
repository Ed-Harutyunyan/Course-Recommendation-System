package edu.aua.course_recommendation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enrollments")
@Getter @Setter
@NoArgsConstructor
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", columnDefinition = "BINARY(16)")
    private User user;

    @ManyToOne
    @MapsId("courseOfferingId")
    @JoinColumn(name = "course_offering_id", columnDefinition = "BINARY(16)")
    private CourseOffering courseOffering;

    @Column(name = "grade", nullable = false)
    private String grade;
}
