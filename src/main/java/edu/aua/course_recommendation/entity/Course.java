package edu.aua.course_recommendation.entity;

import edu.aua.course_recommendation.model.Campus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter @Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String section;

    @Column(nullable = false)
    private String duration;

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Campus campus;

    @ManyToOne
    @JoinColumn(name = "instructor_profile_id", nullable = false)
    private InstructorProfile instructor;

    @Column(nullable = false)
    private String times;

    @Column(nullable = false)
    private String location;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();
}
