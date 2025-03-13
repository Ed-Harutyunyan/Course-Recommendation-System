package edu.aua.course_recommendation.entity;

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
    private Course baseCourse;

    @Column(nullable = false)
    private String section;

    @Column(nullable = false)
    private String session;  // e.g., "15w" for a 15-week course

    @Column(nullable = false)
    private String campus;

    // Association to the Instructor
    @ManyToOne
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(nullable = false)
    private String times;

    @Column
    private String takenSeats;  // e.g., "19/25"

    @Column
    private String spacesWaiting;  // e.g., "0"

    @Column
    private String deliveryMethod;  // e.g., "Online", "Hybrid", "In-Person"

    @Column
    private String distLearning;  // e.g., "Yes" or "No"

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String year;      // e.g., "202425"

    // Semester-specific fields
    @Column(nullable = false)
    private String semester;  // e.g., "1", "2", "3", or "4"

}
