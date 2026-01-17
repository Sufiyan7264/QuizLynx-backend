package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.ExploreDtos;
import com.projectJava.quizApp.DTO.QuizDto;
import com.projectJava.quizApp.DTO.QuizResultDto;
import com.projectJava.quizApp.model.*;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.service.ExploreService;
import com.projectJava.quizApp.service.QuizResultService;
import com.projectJava.quizApp.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizResultService quizResultService;

    @Autowired
    private UserRepo customerRepo;

    @Autowired
    private ExploreService exploreService;

    // 1. Create Quiz (Instructor Only)
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<QuizDto> createQuiz(@RequestBody QuizDto quizDto, Principal principal) {
        Long userId = getUserId(principal);
        return ResponseEntity.ok(quizService.createQuiz(quizDto, userId));
    }

    // 2. Get Quizzes for a Batch (For Student/Instructor Lists)
    @GetMapping("/batch/{batchId}")
    @PreAuthorize("hasAnyRole('STUDENT','INSTRUCTOR')")
    public ResponseEntity<List<QuizDto>> getQuizzesByBatch(@PathVariable Long batchId, Principal principal) {
        Long userId = getUserId(principal);
        return ResponseEntity.ok(quizService.getQuizzesByBatch(batchId, userId));
    }

    // 3. NEW: Start Quiz Info (Metadata for Instructions Page)
    @GetMapping("/start/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','USER')")
    public ResponseEntity<QuizDto> startQuiz(@PathVariable Long id) {
        // Returns Title, Description, Timer, Marks (No Questions)
        return ResponseEntity.ok(quizService.startQuiz(id));
    }
    @PostMapping("/{quizId}/start")
    @PreAuthorize("hasAnyRole('STUDENT','USER')")
    public ResponseEntity<?> startQuiz(@PathVariable Long quizId, Principal principal) {
        quizService.startQuizSession(quizId, principal.getName());
        return ResponseEntity.ok(Map.of("message","timer started"));
    }

    // 4. Get Actual Questions (Exam Mode)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','USER')")
    public ResponseEntity<List<QuestionWrapper>> getQuizQuestion(@PathVariable Long id, Principal principal){
        return quizService.getQuizQuestion(id);
    }

    // 5. Submit
// 5. Submit Quiz
    @PostMapping("/submit/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','USER')")
    public ResponseEntity<QuizResultDto> submitQuiz(@PathVariable Long id,
                                                    @RequestBody List<Response> responses,
                                                    Principal principal) {
        // This now returns the object containing the new Result ID
        return ResponseEntity.ok(quizService.submitQuiz(id, responses, principal.getName()));
    }

    // 6. Update Quiz
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<QuizDto> updateQuiz(@PathVariable Long id, @RequestBody QuizDto quizDto, Principal principal) {
        Long userId = getUserId(principal);
        return ResponseEntity.ok(quizService.updateQuiz(id, quizDto, userId));
    }

    // 7. Get All My Quizzes (Instructor Global List)
    @GetMapping("/all")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<QuizDto>> getAllMyQuizzes(Principal principal) {
        Long userId = getUserId(principal);
        return ResponseEntity.ok(quizService.getAllMyQuizzes(userId));
    }

    // 8. Get Single Quiz (Instructor Edit)
    @GetMapping("/get/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<QuizDto> getMyQuizzes(Principal principal, @PathVariable Long id) {
        Long userId = getUserId(principal);
        return ResponseEntity.ok(quizService.getMyQuizzez(userId, id));
    }

    // Helper
    private Long getUserId(Principal principal) {
        String name = principal.getName();
        return customerRepo.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
    // 1. For loadResult() - Get specific submission by its ID
    @GetMapping("/result/{submissionId}")
    public ResponseEntity<QuizResultDto> getResultById(@PathVariable Long submissionId) {
        QuizResultDto result = quizResultService.getResultBySubmissionId(submissionId);
        return ResponseEntity.ok(result);
    }

    // 2. For loadResultByQuizId() - Get latest submission for a quiz & student
// 2. For loadResultByQuizId() - Get latest submission for a quiz & Current Logged-in Student
    @GetMapping("/{quizId}/result")
    @PreAuthorize("hasRole('STUDENT')") // Ensure only students access this
    public ResponseEntity<QuizResultDto> getLatestResultByQuizId(
            @PathVariable Long quizId,
            Principal principal // <--- CRITICAL CHANGE: Get actual user
    ) {
        // Now we pass the real username (e.g., "john@example.com") instead of "STUDENT_001"
        QuizResultDto result = quizResultService.getLatestResultForStudent(quizId, principal.getName());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-attempts")
    @PreAuthorize("hasAnyRole('STUDENT','USER')")
    public ResponseEntity<Page<QuizResultDto>> getMyAttempts(Principal principal, @RequestParam(defaultValue = "10") int size, @RequestParam (defaultValue = "0") int page) {
        return ResponseEntity.ok(quizResultService.getMyAttempts(principal.getName(),page,size));
    }
    @GetMapping("/student/active-quizzes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<QuizDto>> getMyActiveQuizzes(Principal principal) {
        return ResponseEntity.ok(quizService.getStudentActiveQuizzes(principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id, Principal principal) {
        // We need the user ID to verify ownership
        Customer instructor = customerRepo.findByUsername(principal.getName()).orElseThrow();

        quizService.deleteQuiz(id, instructor.getId());
        return ResponseEntity.ok(Map.of("message","Quiz deleted successfully"));
    }
    @GetMapping("/{quizId}/attempt-status")
    @PreAuthorize("hasAnyRole('STUDENT','USER')")
    public ResponseEntity<Map<String, Object>> getAttemptStatus(@PathVariable Long quizId, Principal principal) {
        Map<String, Object> status = quizService.getAttemptStatus(quizId, principal.getName());

        // If status is null, it means no active attempt exists (User hasn't started yet)
        return ResponseEntity.ok(status);
    }

    @PostMapping("/{quizId}/pause")
    @PreAuthorize("hasAnyRole('STUDENT','USER')")
    public ResponseEntity<?> pauseTimer(
            @PathVariable Long quizId,
            @RequestBody Map<String, Integer> payload,
            Principal principal) {

        Integer secondsLeft = payload.get("remainingSeconds");
        quizService.updateTimer(quizId, principal.getName(), secondsLeft);

        return ResponseEntity.ok(Map.of("message", "Progress Saved"));
    }
    @GetMapping("/getFailedQuiz")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMistakeQuiz(Principal principal){
        List<QuizResultDto> mistakeQuiz = quizResultService.getMistakeResults(principal.getName());
        return ResponseEntity.ok(mistakeQuiz);
    }

    @GetMapping("/category/{subject}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ExploreDtos.TrendingQuizDto>> getQuizzesBySubject(@PathVariable String subject) {
        // Reuse your logic to map Quiz -> TrendingQuizDto
        // Ensure you filter by: q.subject = subject AND q.batch IS NULL
        return ResponseEntity.ok(exploreService.getQuizzesBySubject(subject));
    }
}