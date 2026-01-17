package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class QResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which Quiz was taken?
    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    // Who took the quiz? (Your User/Customer entity)
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Customer student;

    // Snapshot of marks (in case the instructor changes the quiz later)
    private Integer totalMarks;
    private Integer scoreObtained;
    private Integer correctAnswers;
    // Inside QResult.java
    @Column(length = 500)
    private String instructorFeedback; // e.g., "Great job!"
    private LocalDateTime startTime;

    private Integer remainingSeconds;
    private LocalDateTime submitDate;
    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL)
    private List<SubmittedAnswer> submittedAnswers = new ArrayList<>();
}