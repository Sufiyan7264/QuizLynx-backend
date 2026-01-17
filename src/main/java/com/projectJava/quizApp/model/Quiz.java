package com.projectJava.quizApp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.projectJava.quizApp.enums.QuizStatus; // Import your new Enum
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000) // Allow longer descriptions
    private String description;

    private Integer timerInMin;

    // === NEW FIELDS ===

    private String subject; // e.g., "Algebra", "Physics"

    private Integer totalMarks;
    private Integer passingMarks;
    private String difficulty;

    // Scheduling
    private LocalDateTime startDate; // When the quiz becomes active
    private LocalDateTime endDate;   // When the quiz expires

    // Lifecycle Status
    @Enumerated(EnumType.STRING) // Stores "DRAFT", "PUBLISHED" as text in DB
    private QuizStatus status = QuizStatus.DRAFT; // Default to DRAFT

    // ==================

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<Question> questions;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    @JsonIgnore
    private Batch batch;
}