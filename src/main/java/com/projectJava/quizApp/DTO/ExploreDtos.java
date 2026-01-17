package com.projectJava.quizApp.DTO;

import lombok.Builder;
import lombok.Data;

public class ExploreDtos {

    @Data
    @Builder
    public static class ExploreCategoryDto {
        private String name;        // e.g. "Java"
        private String description; // e.g. "OOP principles..."
        private String icon;        // e.g. "pi pi-code"
        private String tag;         // e.g. "Programming"
        private long quizCount;     // Extra: Show how many quizzes exist
    }

    @Data
    @Builder
    public static class TrendingQuizDto {
        private Long id;            // To start the quiz
        private String title;
        private String category;    // Subject
        private String difficulty;  // "Easy", "Medium", "Hard"
        private long attempts;      // Based on QResult count
        private double rating;      // Mocked or calculated
    }
}