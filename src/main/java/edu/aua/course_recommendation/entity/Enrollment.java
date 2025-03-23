package edu.aua.course_recommendation.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @MapsId("courseId")
    @JoinColumn(name = "course_id", columnDefinition = "BINARY(16)")
    @JsonIgnoreProperties
    private Course course;

    @Column(name = "grade")
    private String grade;

    @Column(name = "year") // e.g., "202425"
    private String year;

    @Column(name = "semester") // e.g., "1" for Fall, "2" for Spring, "3" for Summer
    private String semester;

}
