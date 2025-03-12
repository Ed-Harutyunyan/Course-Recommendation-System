package edu.aua.course_recommendation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
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

    @ElementCollection
    @CollectionTable(name = "course_prerequisites",
            joinColumns = @JoinColumn(name = "course_code", referencedColumnName = "code"))
    @Column(name = "prerequisite_code")
    @Builder.Default
    private Set<String> prerequisites = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "course_clusters", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "cluster")
    private List<Integer> clusters = new ArrayList<>();

    @OneToMany(mappedBy = "baseCourse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseOffering> offerings = new ArrayList<>();
}
