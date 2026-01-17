package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
@Data
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_profile_id")
    private InstructorProfile instructor;
    private String description;

    private String batchName; // "Class 8"
    private String batchCode; // "C8-1234"

    private LocalDateTime createdAt;
}