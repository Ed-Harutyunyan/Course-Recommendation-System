package edu.aua.course_recommendation.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "course_offerings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Reference to the base Course
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "offerings"})
    private Course baseCourse;

    @Column(nullable = false)
    private String section;

    @Column(nullable = false)
    private String session;

    @Column(nullable = false)
    private String campus;

    @ManyToOne
    @JoinColumn(name = "instructor_id", nullable = false)
    @JsonIgnoreProperties("courseOfferings")
    private Instructor instructor;

    @Column(nullable = false)
    private String times;

    @Column
    private String takenSeats;

    @Column
    private String spacesWaiting;

    @Column
    private String deliveryMethod;

    @Column
    private String distLearning;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String year;

    @Column(nullable = false)
    private String semester;

}
