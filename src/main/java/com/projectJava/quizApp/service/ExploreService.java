package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.ExploreDtos.*;
import com.projectJava.quizApp.enums.QuizStatus;
import com.projectJava.quizApp.model.Quiz;
import com.projectJava.quizApp.repo.QResultRepo;
import com.projectJava.quizApp.repo.QuizRepo;
import org.springframework.beans.factory.annotation.Autowired;
// --- FIX 1: Correct Imports ---
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExploreService {

    @Autowired private QuizRepo quizRepo;
    @Autowired private QResultRepo qResultRepo;

    private final List<String> CORE_CATEGORIES = Arrays.asList(
            "Java", "Python", "History", "Math", "Science", "General Knowledge"
    );

    // 1. Get Categories
    public List<ExploreCategoryDto> getCategories() {
        return CORE_CATEGORIES.stream().map(coreSubject -> {
            long count = quizRepo.countBySubjectIgnoringCase(coreSubject);
            if (count == 0) return null;
            return mapSubjectToCategory(coreSubject);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // 2. Get Trending Quizzes (FIXED PAGINATION)
    public Page<TrendingQuizDto> getTrendingQuizzes(int pageNumber, int size) {
        // --- FIX 2: Create PageRequest ---
        Pageable pageable = PageRequest.of(pageNumber, size);

        // Fetch Page<Quiz>
        Page<Quiz> quizPage = quizRepo.findByStatus(QuizStatus.PUBLISHED, pageable);

        // --- FIX 3: Use .map() directly on the Page object ---
        // This preserves the pagination metadata (total pages, total elements)
        return quizPage.map(quiz -> {
            long attempts = qResultRepo.countByQuizId(quiz.getId());

            String diff = quiz.getDifficulty();
            // Handle missing difficulty
            if (diff == null) {
                if (quiz.getTotalMarks() > 0) {
                    double ratio = (double) quiz.getPassingMarks() / quiz.getTotalMarks();
                    if (ratio < 0.4) diff = "Easy";
                    else if (ratio > 0.7) diff = "Hard";
                    else diff = "Medium";
                } else {
                    diff = "Medium";
                }
            }

            return TrendingQuizDto.builder()
                    .id(quiz.getId())
                    .title(quiz.getTitle())
                    .category(quiz.getSubject())
                    .difficulty(diff)
                    .attempts(attempts)
                    .rating(4.5)
                    .build();
        });
    }

    public List<TrendingQuizDto> getQuizzesBySubject(String subject) {
        List<Quiz> quizzes = quizRepo.findBySubjectIgnoreCaseAndBatchIsNull(subject);
        return mapToTrendingDto(quizzes);
    }

    // Helper: Map Subject String to Icon
    private ExploreCategoryDto mapSubjectToCategory(String subject) {
        String lower = subject.toLowerCase();
        String desc = "Explore quizzes related to " + subject;
        String icon = "pi pi-file";
        String tag = "General";

        if (lower.contains("java")) {
            desc = "Object-oriented programming, collections, and streams.";
            icon = "pi pi-code";
            tag = "Programming";
        } else if (lower.contains("python")) {
            desc = "Data structures, algorithms, and automation.";
            icon = "pi pi-bolt";
            tag = "Programming";
        } else if (lower.contains("math") || lower.contains("algebra")) {
            desc = "Calculus, algebra, and quantitative aptitude.";
            icon = "pi pi-calculator";
            tag = "STEM";
        } else if (lower.contains("history")) {
            desc = "World events, civilizations, and modern history.";
            icon = "pi pi-globe";
            tag = "Humanities";
        }

        return ExploreCategoryDto.builder()
                .name(subject).description(desc).icon(icon).tag(tag).build();
    }

    // Helper: Map List<Quiz> to List<DTO>
    private List<TrendingQuizDto> mapToTrendingDto(List<Quiz> quizzes) {
        return quizzes.stream().map(quiz -> {
            long attempts = qResultRepo.countByQuizId(quiz.getId());
            String diff = quiz.getDifficulty();
            if (diff == null) diff = "Medium";

            return TrendingQuizDto.builder()
                    .id(quiz.getId())
                    .title(quiz.getTitle())
                    .category(quiz.getSubject())
                    .difficulty(diff)
                    .attempts(attempts)
                    .rating(4.5)
                    .build();
        }).collect(Collectors.toList());
    }
}