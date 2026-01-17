package com.projectJava.quizApp.enums;

public enum QuizStatus {
    DRAFT,      // Instructor is still editing, students can't see it
    PUBLISHED,  // Visible to students (if date is valid)
    CLOSED      // No longer accepting responses
}
