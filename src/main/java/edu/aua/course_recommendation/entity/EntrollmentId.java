package edu.aua.course_recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter
@NoArgsConstructor
public class EntrollmentId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "student_profile_id", columnDefinition = "BINARY(16)")
    private UUID studentProfileId;

    @Column(name = "course_id", columnDefinition = "BINARY(16)")
    private UUID courseId;


}
