package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.Question;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.service.QuestionService;
import com.projectJava.quizApp.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private QuizService quizService;

    // 1. Get Questions for a specific Quiz
    // Returns a List (Array), which is valid JSON
    @GetMapping("/quiz/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<Question>> getQuestionsByQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(questionService.getQuestionsByQuizId(quizId));
    }

    // 2. Add Question to a specific Quiz
    // Returns {"message": "Question Created Successfully"}
    @PostMapping("/add/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<Map<String, String>> addQuestion(@PathVariable Long quizId, @RequestBody Question question) {
        try {
            questionService.addQuestion(quizId, question);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Question Created Successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Update
    // FIXED: Returns {"message": "Question Updated Successfully"} instead of plain string
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<Map<String, String>> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        try {
            questionService.updateQuestion(id, question);
            return ResponseEntity.ok(Map.of("message", "Question Updated Successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 5. Get Single Question (For Editing)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionById(id));
    }
    // 4. Delete
    // FIXED: Returns {"message": "Question Deleted Successfully"} instead of plain string
//    @DeleteMapping("/delete/{id}")
//    @PreAuthorize("hasRole('INSTRUCTOR')")
//    public ResponseEntity<Map<String, String>> deleteQuestion(@PathVariable Long id) {
//        try {
//            questionService.deleteQuestion(id);
//            return ResponseEntity.ok(Map.of("message", "Question Deleted Successfully"));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id, Principal principal) {
        Customer instructor = userRepo.findByUsername(principal.getName()).orElseThrow();

        quizService.deleteQuestion(id, instructor.getId());
        return ResponseEntity.ok(Map.of("message","Question deleted successfully"));
    }
}