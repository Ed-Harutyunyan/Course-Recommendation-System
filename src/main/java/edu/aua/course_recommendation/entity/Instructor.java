package edu.aua.course_recommendation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "instructors")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String imageUrl;
    private String position;
    private String mobile;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String officeLocation;

    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseOffering> courseOfferings;
}
