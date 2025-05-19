package edu.aua.course_recommendation.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "schedules")
@Data
@Builder
@AllArgsConstructor @NoArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "schedule_name", nullable = false)
    private String scheduleName;

    @Column(name = "user_id", nullable = false)
    private UUID studentId;

    @ElementCollection
    @CollectionTable(
            name = "schedule_slots",
            joinColumns = @JoinColumn(name = "schedule_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<ScheduleSlot> slots;
}
