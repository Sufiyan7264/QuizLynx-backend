package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.BatchQuizResultDto;
import com.projectJava.quizApp.service.BatchAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/analytics")
public class BatchAnalyticsController {

    @Autowired
    private BatchAnalyticsService batchAnalyticsService;

    // Matches your Angular call: getBatchResults(batchId, quizId)
    @GetMapping("/batch/{batchId}/quiz/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<BatchQuizResultDto> getBatchQuizAnalytics(
            @PathVariable Long batchId,
            @PathVariable Long quizId,
            Principal principal) {

        return ResponseEntity.ok(
                batchAnalyticsService.getBatchQuizResults(batchId, quizId, principal.getName())
        );
    }
}