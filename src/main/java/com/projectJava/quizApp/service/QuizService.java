package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.QuizDto;
import com.projectJava.quizApp.DTO.QuizResultDto;
import com.projectJava.quizApp.enums.QuizStatus;
import com.projectJava.quizApp.model.*;
import com.projectJava.quizApp.repo.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuizService {
    @Autowired
    private QuizRepo quizRepo;
    @Autowired
    private BatchRepo batchRepo;
    @Autowired
    private QResultRepo qResultRepo;
    @Autowired
    private AssignmentRepo assignmentRepo;
    @Autowired
    private QuestionRepo questionRepo;
    @Autowired
    private UserRepo customerRepo; // or CustomerRepo

    @Transactional
    public void startQuizSession(Long quizId, String username) {
        Customer student = customerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<QResult> existingAttempt = qResultRepo.findByStudentIdAndQuizIdAndSubmitDateIsNull(student.getId(), quizId);

        if (existingAttempt != null && !existingAttempt.isEmpty()) {
            return; // Already started
        }

        QResult attempt = new QResult();
        attempt.setStudent(student);
        attempt.setQuiz(quiz);
        attempt.setStartTime(LocalDateTime.now());
        attempt.setScoreObtained(0);
        attempt.setTotalMarks(quiz.getTotalMarks());

        // NEW: Set initial remaining time (Minutes * 60)
        attempt.setRemainingSeconds(quiz.getTimerInMin() * 60);

        qResultRepo.save(attempt);
    }

    @Transactional
    public void updateTimer(Long quizId, String username, Integer secondsLeft) {
        Customer student = customerRepo.findByUsername(username).orElseThrow();

        // Fetch the ACTIVE attempt
        List<QResult> attempts = qResultRepo.findByStudentIdAndQuizIdAndSubmitDateIsNull(student.getId(), quizId);

        if (!attempts.isEmpty()) {
            QResult current = attempts.get(0);
            current.setRemainingSeconds(secondsLeft);
            qResultRepo.save(current);
        }
    }
    public List<QuizDto> getStudentActiveQuizzes(String username) {
        // 1. Find Student
        Customer student = customerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Find all Batches the student is assigned to
        // Using your existing 'InstructorStudentAssignment' entity
        List<InstructorStudentAssignment> assignments = assignmentRepo.findByStudentId(student.getId());

        if (assignments.isEmpty()) {
            return new ArrayList<>(); // Student hasn't joined any classes yet
        }

        // 3. Extract the Batch IDs
        List<Long> batchIds = assignments.stream()
                .map(assignment -> assignment.getBatch().getId())
                .collect(Collectors.toList());

        // 4. Find Active Quizzes for those Batches
        List<Quiz> quizzes = quizRepo.findActiveQuizzesForBatches(batchIds);

        // 5. Convert to DTOs
        return quizzes.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // 1. Create Quiz (Perfect)
    public QuizDto createQuiz(QuizDto req, Long userId) {
        Batch batch = batchRepo.findById(req.getBatchId())
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (!batch.getInstructor().getCustomer().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (req.getStartDate() != null && req.getEndDate() != null &&
                req.getStartDate().isAfter(req.getEndDate())) {
            throw new RuntimeException("Start Date cannot be after End Date");
        }
        if (req.getPassingMarks() > req.getTotalMarks()) {
            throw new RuntimeException("Passing marks cannot exceed Total marks");
        }

        Quiz quiz = new Quiz();
        quiz.setTitle(req.getTitle());
        quiz.setDescription(req.getDescription());
        quiz.setSubject(req.getSubject());
        quiz.setTimerInMin(req.getTimerInMin());
        quiz.setTotalMarks(req.getTotalMarks());
        quiz.setPassingMarks(req.getPassingMarks());
        quiz.setStartDate(req.getStartDate());
        quiz.setEndDate(req.getEndDate());
        quiz.setStatus(req.getStatus() != null ? req.getStatus() : QuizStatus.DRAFT);
        quiz.setBatch(batch);

        Quiz saved = quizRepo.save(quiz);
        return mapToDto(saved);
    }

    // 2. Get Quizzes by Batch (Check Role Comparison)
    public List<QuizDto> getQuizzesByBatch(Long batchId, Long userId) {
        Customer user = customerRepo.findById(userId).orElseThrow();

        // CHECK: If your Role is an Enum, use .name().equals() or == Role.INSTRUCTOR
        boolean isInstructor = user.getRole().toString().equals("INSTRUCTOR");

        List<Quiz> quizzes;
        if (isInstructor) {
            quizzes = quizRepo.findByBatchId(batchId);
        } else {
            quizzes = quizRepo.findActiveQuizzesForStudent(batchId);
        }
        return quizzes.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // 5. Update Quiz Logic
    public QuizDto updateQuiz(Long quizId, QuizDto req, Long userId) {
        // 1. Fetch existing Quiz
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // 2. Security: Check if the user owns the Batch this quiz belongs to
        if (!quiz.getBatch().getInstructor().getCustomer().getId().equals(userId)) {
            throw new RuntimeException("You do not have permission to update this quiz");
        }

        // 3. Validation (Re-check dates and marks)
        if (req.getStartDate() != null && req.getEndDate() != null &&
                req.getStartDate().isAfter(req.getEndDate())) {
            throw new RuntimeException("Start Date cannot be after End Date");
        }
        if (req.getPassingMarks() > req.getTotalMarks()) {
            throw new RuntimeException("Passing marks cannot exceed Total marks");
        }

        // 4. Update Fields
        quiz.setTitle(req.getTitle());
        quiz.setDescription(req.getDescription());
        quiz.setSubject(req.getSubject());
        quiz.setTimerInMin(req.getTimerInMin());
        quiz.setTotalMarks(req.getTotalMarks());
        quiz.setPassingMarks(req.getPassingMarks());
        quiz.setStartDate(req.getStartDate());
        quiz.setEndDate(req.getEndDate());

        // Critical: This allows moving from DRAFT -> PUBLISHED
        quiz.setStatus(req.getStatus());

        // Note: We generally do NOT update the 'Batch' here.
        // Moving quizzes between batches is complex and rarely needed.

        Quiz updatedQuiz = quizRepo.save(quiz);

        return mapToDto(updatedQuiz);
    }

    // 3. Start Quiz (SECURED)
    public ResponseEntity<List<QuestionWrapper>> getQuizQuestion(Long id) {
        Optional<Quiz> quizOpt = quizRepo.findById(id);
        if (quizOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Quiz quiz = quizOpt.get();

        // === SECURITY FIX START ===
        // Prevent students from accessing Drafts or Future quizzes via direct URL
        LocalDateTime now = LocalDateTime.now();

        boolean isPublished = quiz.getStatus() == QuizStatus.PUBLISHED;
        boolean isStarted = quiz.getStartDate() == null || !now.isBefore(quiz.getStartDate());
        boolean isEnded = quiz.getEndDate() != null && now.isAfter(quiz.getEndDate());

        if (!isPublished || !isStarted) {
            // Quiz hasn't started or is still a draft
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // 403 Forbidden
        }
        if (isEnded) {
            return new ResponseEntity<>(HttpStatus.GONE); // 410 Gone (Time expired)
        }
        // === SECURITY FIX END ===

        List<Question> questionsFromDB = quiz.getQuestions();
        List<QuestionWrapper> safeQuestions = new ArrayList<>();

        for (Question q : questionsFromDB) {
            QuestionWrapper wrapper = new QuestionWrapper(
                    q.getId(),
                    q.getQuestionTitle(),
                    q.getOption1(),
                    q.getOption2(),
                    q.getOption3(),
                    q.getOption4(),
                    q.getMarks()
            );
            safeQuestions.add(wrapper);
        }

        return new ResponseEntity<>(safeQuestions, HttpStatus.OK);
    }

    // Inside QuizService

    public List<QuizDto> getAllMyQuizzes(Long userId) {
        // 1. Fetch from Repo
        List<Quiz> quizzes = quizRepo.findAllByInstructorId(userId);

        // 2. Convert to DTOs
        return quizzes.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public QuizDto getMyQuizzez(Long userId, Long id) {
        // 1. Fetch from Repo
        Quiz quizzes = quizRepo.findAllByInstructorIdAndQuizId(userId, id);

        // 2. Convert to DTOs
        return mapToDto(quizzes);
    }


    // 1. Start Quiz (Metadata Only - For Instructions Page)
    public QuizDto startQuiz(Long quizId) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // Basic validation (Optional: Check if date is valid)
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new RuntimeException("This quiz is not published.");
        }

        // Return DTO (Metadata only, no questions)
        return mapToDto(quiz);
    }

    // 2. Map to DTO Helper (Ensure batchName is set)
//    private QuizDto mapToDto(Quiz quiz) {
//        QuizDto dto = new QuizDto();
//        dto.setId(quiz.getId());
//        dto.setTitle(quiz.getTitle());
//        dto.setDescription(quiz.getDescription());
//        dto.setSubject(quiz.getSubject());
//        dto.setTimerInMin(quiz.getTimerInMin());
//        dto.setTotalMarks(quiz.getTotalMarks());
//        dto.setPassingMarks(quiz.getPassingMarks());
//        dto.setStartDate(quiz.getStartDate());
//        dto.setEndDate(quiz.getEndDate());
//        dto.setStatus(quiz.getStatus());
//        dto.setBatchId(quiz.getBatch().getId());
//
//        // Add Batch Name (Useful for UI)

    /// /        if (quiz.getBatch() != null) {
    /// /            dto.setBatchName(quiz.getBatch().getBatchName());
    /// /        }
//
//        dto.setQuestionCount(quiz.getQuestions() != null ? quiz.getQuestions().size() : 0);
//        return dto;
//    }

    // Helper
    private QuizDto mapToDto(Quiz quiz) {
        QuizDto dto = new QuizDto();
        dto.setId(quiz.getId());
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setSubject(quiz.getSubject());
        dto.setTimerInMin(quiz.getTimerInMin());
        dto.setTotalMarks(quiz.getTotalMarks());
        dto.setPassingMarks(quiz.getPassingMarks());
        dto.setStartDate(quiz.getStartDate());
        dto.setEndDate(quiz.getEndDate());
        dto.setStatus(quiz.getStatus());
        if (quiz.getBatch() != null) {
            dto.setBatchId(quiz.getBatch().getId());
            // Optional: Set batch name if your DTO has it
            // dto.setBatchName(quiz.getBatch().getBatchName());
        } else {
            // It's a Global/Public Quiz
            dto.setBatchId(null);
            // dto.setBatchName("Global Library");
        }
        dto.setQuestionCount(quiz.getQuestions() != null ? quiz.getQuestions().size() : 0);
        return dto;
    }

    // 4. Submit Placeholder (Keep existing)
    @Transactional
    public QuizResultDto submitQuiz(Long quizId, List<Response> responses, String name) {

        // 1. Fetch Quiz and Student
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        Customer student = customerRepo.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 2. FIND EXISTING ATTEMPT (The Fix)
        // Instead of 'new QResult()', we look for the one created by startQuizSession
        List<QResult> attempts = qResultRepo.findByStudentIdAndQuizIdAndSubmitDateIsNull(student.getId(), quizId);

        QResult result;

        if (!attempts.isEmpty()) {
            // Found the row created when timer started! Use it.
            result = attempts.get(0);
        } else {
            // Fallback: If no start row exists (e.g. user refreshed DB or bypassed start), create new.
            result = new QResult();
            result.setQuiz(quiz);
            result.setStudent(student);
            result.setStartTime(LocalDateTime.now()); // Approximate start time
        }

        // 3. Update the existing row with submission details
        result.setSubmitDate(LocalDateTime.now()); // STOP THE CLOCK
        result.setTotalMarks(quiz.getTotalMarks());

        int correctAnswersCount = 0;
        int scoreObtained = 0;

        List<SubmittedAnswer> answerEntities = new ArrayList<>();

        // 4. Process Answers
        for (Response userReq : responses) {
            Question question = questionRepo.findById(userReq.getId()).orElse(null);

            if (question != null) {
                // A. Create Answer Entity
                SubmittedAnswer ans = new SubmittedAnswer();
                ans.setQuestion(question);
                ans.setResult(result);
                ans.setSelectedResponse(userReq.getResponse());

                answerEntities.add(ans);

                // B. Check Score
                if (userReq.getResponse() != null &&
                        userReq.getResponse().trim().equalsIgnoreCase(question.getRightAnswer().trim())) {
                    correctAnswersCount++;
                    scoreObtained += (question.getMarks() != null ? question.getMarks() : 1);
                }
            }
        }

        // 5. Finalize and Save
        result.setScoreObtained(scoreObtained);
        result.setCorrectAnswers(correctAnswersCount);

        // If re-submitting, we might want to clear old answers, but usually
        // a student submits only once. Just setting the list is fine for new objects.
        result.setSubmittedAnswers(answerEntities);

        // This effectively UPDATES the existing row because 'result' has an ID
        QResult savedResult = qResultRepo.save(result);

        // 6. Return DTO
        QuizResultDto resultDto = new QuizResultDto();
        resultDto.setQuizId(quiz.getId());
        resultDto.setId(savedResult.getId());
        resultDto.setScore(Double.valueOf(scoreObtained));
        resultDto.setScoreObtained(scoreObtained);
        resultDto.setTotalMarks(quiz.getTotalMarks().doubleValue());

        // Calculate Percentage
        double percentage = 0.0;
        if (quiz.getTotalMarks() > 0) {
            percentage = ((double) scoreObtained / quiz.getTotalMarks()) * 100;
        }
        resultDto.setPercentage(percentage);

        // Pass/Fail Status
        boolean isPassed = scoreObtained >= (quiz.getPassingMarks() != null ? quiz.getPassingMarks() : 0);
        resultDto.setPassed(isPassed);
        resultDto.setResultStatus(isPassed ? "PASS" : "FAIL");

        return resultDto;
    }

    // Inside QuizService.java

    @Transactional
    public void deleteQuiz(Long quizId, Long userId) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // 1. Security Check
        if (!quiz.getBatch().getInstructor().getCustomer().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this quiz");
        }

        // 2. SAFETY CHECK: Has anyone taken this quiz?
        long submissionCount = qResultRepo.countByQuizId(quizId);

        if (submissionCount > 0) {
            // OPTION A: Block the delete (Recommended)
            throw new RuntimeException("Cannot delete: " + submissionCount + " students have already submitted this quiz. Please 'Close' or 'Archive' it instead.");

            // OPTION B: Soft Delete (Advanced)
            // quiz.setDeleted(true);
            // quizRepo.save(quiz);
            // return;
        }

        // 3. If NO submissions, it is safe to hard delete
        quizRepo.delete(quiz);
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        Question question = questionRepo.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!question.getQuiz().getBatch().getInstructor().getCustomer().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // 1. SAFETY CHECK: Has this question been answered?
        // You need a repository method for this
        boolean isAnswered = qResultRepo.existsByQuestionId(questionId);

        if (isAnswered) {
            throw new RuntimeException("Cannot delete: Students have already answered this question. You can only edit the text.");
        }

        // 2. Safe to delete
        // Optional: Recalculate Quiz Totals here if you want
        questionRepo.delete(question);
    }

    public Map<String, Object> getAttemptStatus(Long quizId, String username) {
        Customer student = customerRepo.findByUsername(username).orElseThrow();
        List<QResult> activeAttempts = qResultRepo.findByStudentIdAndQuizIdAndSubmitDateIsNull(student.getId(), quizId);

        if (!activeAttempts.isEmpty()) {
            QResult currentAttempt = activeAttempts.get(0);

            // Return the EXACT saved seconds, not a date calculation
            return Map.of(
                    "remainingSeconds", currentAttempt.getRemainingSeconds()
            );
        }
        return null;
    }}