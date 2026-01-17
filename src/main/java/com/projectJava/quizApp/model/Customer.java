package com.projectJava.quizApp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String role;
    private String email;
    private String country="GLOBAL";
    private Boolean verified=false;

    private Instant deletedAt;

    private Integer aiUsageCount=0;
    private LocalDate lastAiUsageDate;


    // OAuth2 specific fields
    private String provider;    // e.g., "google"
    private String providerId;  // provider’s unique ID for the user
    private Instant passwordChangedAt;

}
