package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "quiz_submissions")
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which quiz was this?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    // Who took it? (Storing ID as string, assuming you might use UUIDs or external IDs)
    private String studentId;

    // Scoring details
    private Double score;
    private Double totalMarks;

    // Timing
    private LocalDateTime startTime;
    private LocalDateTime submitTime;

    // The individual answers the student gave in this attempt
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizAnswer> answers = new ArrayList<>();

    // Helper method to add answer easily
    public void addAnswer(QuizAnswer answer) {
        this.answers.add(answer);
        answer.setSubmission(this);
    }
}