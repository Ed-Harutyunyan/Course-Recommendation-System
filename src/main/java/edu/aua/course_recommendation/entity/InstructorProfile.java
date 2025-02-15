package edu.aua.course_recommendation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "instructor_profiles")
@Getter @Setter
@NoArgsConstructor
public class InstructorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> coursesTaught = new ArrayList<>();
}
