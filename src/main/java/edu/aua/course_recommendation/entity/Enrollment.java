package edu.aua.course_recommendation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id;

    @ManyToOne
    @MapsId("studentId")
    @JoinColumn(name = "student_id", columnDefinition = "BINARY(16)")
    private Student student;

    @ManyToOne
    @MapsId("courseId")
    @JoinColumn(name = "course_id", columnDefinition = "BINARY(16)")
    private Course course;

    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "semester", nullable = false)
    private String semester;
}
