package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    // Link to your User (Customer)
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Customer customer;
}