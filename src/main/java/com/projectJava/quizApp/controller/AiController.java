package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.AiGenerateRequest;
import com.projectJava.quizApp.DTO.BulkSaveRequest;
import com.projectJava.quizApp.DTO.QuestionDto;
import com.projectJava.quizApp.model.Question;
import com.projectJava.quizApp.service.AiQuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiQuizService aiQuizService;

    @PostMapping("/preview")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<QuestionDto>> previewQuestions(@RequestBody AiGenerateRequest request,Principal principal) {
        return ResponseEntity.ok(aiQuizService.generateQuestionPreview(request,principal.getName()));
    }
    @PostMapping("/generate-from-topic")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Long>> generateFromTopic(@RequestBody com.projectJava.quizApp.DTO.AiTopicRequest request, Principal principal) {
        Long quizId = aiQuizService.createQuizFromTopic(request,principal.getName());
        aiQuizService.checkAndIncrementLimit(principal.getName());
        return ResponseEntity.ok(Map.of("quizId", quizId));
    }

    @PostMapping("/save-bulk")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','USER')")
    public ResponseEntity<?> saveBulkQuestions(@RequestBody BulkSaveRequest request) {
        aiQuizService.saveQuestionsToQuiz(request.getQuizId(), request.getQuestions());
        return ResponseEntity.ok(Map.of("message","Questions saved successfully"));
    }
    @GetMapping("/usage")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','USER')")
    public ResponseEntity<?> getAiUsage(Principal principal) {
        return ResponseEntity.ok(aiQuizService.getUsageStats(principal.getName()));
    }
}