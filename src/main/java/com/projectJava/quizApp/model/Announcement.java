package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String message;

    private LocalDateTime date;

    private String priority; // "high", "medium", "low"

    // Linked to an Instructor
    @ManyToOne
    @JoinColumn(name = "instructor_id")
    private InstructorProfile instructor;

    // Linked to a Batch (so only students in that batch see it)
    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;
}