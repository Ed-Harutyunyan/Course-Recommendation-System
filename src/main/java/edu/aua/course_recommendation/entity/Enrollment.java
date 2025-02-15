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
    private EntrollmentId id;

    @ManyToOne
    @MapsId("studentProfileId")
    @JoinColumn(name = "student_profile_id", columnDefinition = "BINARY(16)")
    private StudentProfile studentProfile;

    @ManyToOne
    @MapsId("courseId")
    @JoinColumn(name = "course_id", columnDefinition = "BINARY(16)")
    private Course course;

    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "semester", nullable = false)
    private String semester;
}
