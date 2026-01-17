package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "instructor_profile")
@NoArgsConstructor
@AllArgsConstructor
public class InstructorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false,unique = true)
    private Customer customer;
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String[] subjects;

    @Column(unique = true)
    private String instructorCode;

    private String avatarUrl;

    private LocalDate createdAt;
    private LocalDate updatedAt;

    @PrePersist
    public void prePersist(){
        if (createdAt == null) createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

}
