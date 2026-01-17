package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.ExploreDtos.*;
import com.projectJava.quizApp.DTO.LeaderboardDto;
import com.projectJava.quizApp.service.ExploreService;
import com.projectJava.quizApp.service.LeaderboardService;
import com.projectJava.quizApp.service.QuizGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/explore")
@PreAuthorize("hasRole('USER')")
public class ExploreController {
    @Autowired private QuizGeneratorService generatorService;

    @Autowired
    private ExploreService exploreService;
    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/categories")
    public ResponseEntity<List<ExploreCategoryDto>> getCategories() {
        return ResponseEntity.ok(exploreService.getCategories());
    }

    @GetMapping("/trending")
    public ResponseEntity<Page<TrendingQuizDto>> getTrending(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(exploreService.getTrendingQuizzes(page,size));
    }
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, String> request) {
        String category = request.get("category"); // e.g., "History"
        String difficulty = request.get("difficulty"); // e.g., "medium"

        Long quizId = generatorService.generateInstantQuiz(category, difficulty);

        return ResponseEntity.ok(Map.of("quizId", quizId));
    }
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardDto>> getLeaderboard(){
        return ResponseEntity.ok(leaderboardService.getLeaderboardData());
    }
}