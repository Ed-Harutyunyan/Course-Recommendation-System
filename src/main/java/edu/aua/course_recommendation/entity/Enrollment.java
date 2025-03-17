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

    // TODO: This should probably be changed to store base course instead of offering
    // How would students choose previous offerings?
    // They can only choose from base courses
    // Let's then use CourseOfferings for just registration / base course creation logic
    // and allow here to optionally set what grade, year and semester they took the course
    // MAYBE even a boolean to check if completed and maybe separately instructor
    // Also will allow creating a custom course (enrollment) and keep this all client side
    @ManyToOne
    @MapsId("courseId")
    @JoinColumn(name = "course_id", columnDefinition = "BINARY(16)")
    private Course course;

    @Column(name = "grade")
    private String grade;

    @Column(name = "year") // e.g., "202425"
    private String year;

    @Column(name = "semester") // e.g., "1" for Fall, "2" for Spring, "3" for Summer
    private String semester;

}
