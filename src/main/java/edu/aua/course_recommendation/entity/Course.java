package edu.aua.course_recommendation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer credits;

    @ManyToMany
    @JoinTable(
            name = "course_prerequisites",
            joinColumns = @JoinColumn(name = "course_code"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_code")
    )
    private Set<Course> prerequisites = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "course_clusters", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "cluster")
    private List<Integer> clusters = new ArrayList<>();

    @OneToMany(mappedBy = "baseCourse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseOffering> offerings = new ArrayList<>();
}
