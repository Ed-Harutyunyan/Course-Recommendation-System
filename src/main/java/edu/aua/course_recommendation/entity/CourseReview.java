package edu.aua.course_recommendation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int rating; // 1-5 stars

    @Column(nullable = false)
    private LocalDateTime createdAt;
}