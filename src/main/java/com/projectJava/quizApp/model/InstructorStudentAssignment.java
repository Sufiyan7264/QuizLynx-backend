package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.Long;
import java.time.LocalDateTime;

@Data
@Table(name = "instructor_students_assignments")
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class InstructorStudentAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private Customer student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_profile_id", nullable = false)
    private InstructorProfile instructorProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    private LocalDateTime assignedAt;
    private boolean active = true;
    private String notes;

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }
}
