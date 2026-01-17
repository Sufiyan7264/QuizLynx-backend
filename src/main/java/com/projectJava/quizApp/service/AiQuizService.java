package com.projectJava.quizApp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectJava.quizApp.DTO.AiGenerateRequest;
import com.projectJava.quizApp.DTO.AiTopicRequest;
import com.projectJava.quizApp.DTO.QuestionDto;
import com.projectJava.quizApp.enums.QuizStatus;
import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.Question;
import com.projectJava.quizApp.model.Quiz;
import com.projectJava.quizApp.repo.QResultRepo;
import com.projectJava.quizApp.repo.QuestionRepo;
import com.projectJava.quizApp.repo.QuizRepo;
import com.projectJava.quizApp.repo.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiQuizService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Autowired private QuestionRepo questionRepo;
    @Autowired private QuizRepo quizRepo;
    @Autowired private QResultRepo qResultRepo;
    @Autowired private UserRepo userRepo;

    // 1. GENERATE PREVIEW (Do not save to DB)
    public List<QuestionDto> generateQuestionPreview(AiGenerateRequest request,String username) {
        String prompt = String.format(
                "Generate %d multiple choice questions based on: '%s'. " +
                        "Return RAW JSON Array only. Schema: " +
                        "{ \"questionTitle\": \"...\", \"option1\": \"...\", \"option2\": \"...\", \"option3\": \"...\", \"option4\": \"...\", \"rightAnswer\": \"...\", \"difficultyLevel\": \"Medium\" }",
                request.getNumberOfQuestions(), request.getContentText()
        );

        String jsonResponse = callGeminiApi(prompt);
        checkAndIncrementLimit(username);

        // Parse directly to DTOs
        return parseToDtoList(jsonResponse);
    }

    // 2. SAVE TO QUIZ (Called after review)
    @Transactional // Ensure all DB operations happen together
    public List<Question> saveQuestionsToQuiz(Long quizId, List<QuestionDto> dtos) {
        // 1. Fetch the Quiz
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // 2. Fetch EXISTING questions to calculate "Used Budget"
        List<Question> existingQuestions = questionRepo.findByQuizId(quizId);

        int usedMarks = existingQuestions.stream()
                .mapToInt(q -> q.getMarks() != null ? q.getMarks() : 0)
                .sum();

        int totalQuizMarks = quiz.getTotalMarks();
        int remainingMarks = totalQuizMarks - usedMarks;
        int newQuestionCount = dtos.size();

        // 3. CRITICAL VALIDATION
        // Ensure we have enough marks left. We need at least 1 mark per question ideally,
        // but the math works even if remainingMarks < count (some questions get 0).
        // However, it's better to block it if marks are too low to avoid 0-mark questions.
        if (remainingMarks < newQuestionCount) {
            throw new RuntimeException(
                    String.format("Not enough marks! You have %d marks remaining for %d new questions. Please increase Total Marks in Quiz Settings.",
                            remainingMarks, newQuestionCount)
            );
        }

        // 4. Distribute Logic (Only for New Questions)
        int baseScore = remainingMarks / newQuestionCount;
        int remainder = remainingMarks % newQuestionCount;

        List<Question> newQuestions = new ArrayList<>();

        for (int i = 0; i < newQuestionCount; i++) {
            QuestionDto dto = dtos.get(i);
            Question q = new Question();

            // Map Fields
            q.setQuestionTitle(dto.getQuestionTitle());
            q.setOption1(dto.getOption1());
            q.setOption2(dto.getOption2());
            q.setOption3(dto.getOption3());
            q.setOption4(dto.getOption4());
            q.setRightAnswer(dto.getRightAnswer());
            q.setQuiz(quiz);

            // Assign Marks:
            // Give baseScore to everyone.
            // Give +1 extra to the first few questions to use up the remainder.
            int assignedMark = baseScore;
            if (i < remainder) {
                assignedMark++;
            }
            q.setMarks(assignedMark);

            newQuestions.add(q);
        }

        // 5. Save ONLY new questions
        return questionRepo.saveAll(newQuestions);
    }
    private void recalculateQuizMarks(Quiz quiz) {
        List<Question> allQuestions = questionRepo.findByQuizId(quiz.getId());

        if (allQuestions.isEmpty()) return;

        int totalQuizMarks = quiz.getTotalMarks(); // e.g., 100
        int questionCount = allQuestions.size();   // e.g., 20

        if (questionCount > 0) {
            // Calculate base mark (e.g., 100 / 20 = 5)
            int evenMark = totalQuizMarks / questionCount;
            int remainder = totalQuizMarks % questionCount;

            for (int i = 0; i < allQuestions.size(); i++) {
                Question q = allQuestions.get(i);

                // Assign base mark
                int assignedMark = evenMark;

                // Distribute remainder to the first few questions to ensure total matches exactly
                // e.g., if 100 marks / 3 questions = 33, 33, 33. Remainder 1.
                // Result: 34, 33, 33.
                if (remainder > 0) {
                    assignedMark += 1;
                    remainder--;
                }

                q.setMarks(assignedMark);
            }

            // Save updated marks for ALL questions
            questionRepo.saveAll(allQuestions);
        }
    }
    // Helper to parse JSON to DTO
    private List<QuestionDto> parseToDtoList(String jsonString) {
        try {
            String cleanJson = jsonString.replaceAll("```json", "").replaceAll("```", "").trim();
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(cleanJson, new TypeReference<List<QuestionDto>>(){});
        } catch (Exception e) {
            throw new RuntimeException("AI Parsing Error: " + e.getMessage());
        }
    }

    private String callGeminiApi(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        // Gemini Request Structure
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(Map.of("text", prompt)));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(content));

        // URL with Key
        String finalUrl = apiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(finalUrl, entity, Map.class);

            // Extract text from deeply nested JSON response
            Map<String, Object> respBody = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) respBody.get("candidates");
            Map<String, Object> contentPart = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) contentPart.get("parts");

            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            throw new RuntimeException("Error calling AI API: " + e.getMessage());
        }
    }

    private List<Question> parseQuestions(String jsonString) {
        try {
            // Clean up Markdown if AI adds it (e.g. ```json ... ```)
            String cleanJson = jsonString.replaceAll("```json", "").replaceAll("```", "").trim();

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(cleanJson, new TypeReference<List<Question>>(){});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage());
        }
    }
    // 3. NEW: Generate & Save Quiz from TOPIC (For Individual Users)
    @Transactional
    public synchronized Long createQuizFromTopic(AiTopicRequest request, String username) {

        // Base Title: "Practice: Java (Hard)"
// 1. Normalize the Subject (Fixes "java" -> "Java")
        String finalSubject = normalizeSubject(request.getTopic().trim());

        // 2. Build Base Title using the NORMALIZED subject
        // This ensures "Practice: java (Hard)" matches "Practice: Java (Hard)" in DB
        String baseTitle = "Practice: " + finalSubject + " (" + request.getDifficulty() + ")";
        // 1. Fetch ALL existing versions of this quiz
        List<Quiz> existingQuizzes = quizRepo.findByTitleStartingWithAndBatchIsNull(baseTitle);

        // 2. Fetch IDs of quizzes this user has ALREADY taken
        List<Long> attemptedIds = qResultRepo.findAttemptedQuizIdsByStudent(username);

        // 3. CHECK: Is there an existing version they haven't taken yet?
        for (Quiz q : existingQuizzes) {
            if (!attemptedIds.contains(q.getId())) {
                System.out.println("User hasn't played Quiz ID " + q.getId() + " yet. Returning it.");
                return q.getId(); // Return existing unused quiz
            }
        }

        // Calculate Version Number (e.g., if we have #1 and #2, make #3)
        int nextVersion = existingQuizzes.size() + 1;
        String newTitle = baseTitle + " #" + nextVersion;

        System.out.println("User finished all existing versions. Generating: " + newTitle);

        try {
            // A. Create Container
            Quiz quiz = new Quiz();
            quiz.setTitle(newTitle); // Title includes #2, #3, etc.
            quiz.setDescription("AI-Generated practice quiz on " + request.getTopic());
            quiz.setSubject(finalSubject);
            quiz.setDifficulty(request.getDifficulty());
            quiz.setStatus(QuizStatus.valueOf("PUBLISHED"));
            quiz.setTimerInMin(request.getQuestions());
            quiz.setTotalMarks(request.getQuestions() * 5);
            quiz.setBatch(null); // Public

            quiz = quizRepo.save(quiz);

            // B. Build Prompt (Ask for DIFFERENT questions if possible, though LLM is random)
// B. Build Prompt with Strict JSON Schema
            String prompt = String.format(
                    "Generate %d multiple choice questions on the topic: '%s'. " +
                            "Difficulty level: %s. " +
                            "Return RAW JSON Array only. No Markdown. Schema: " +
                            "{ \"questionTitle\": \"...\", \"option1\": \"...\", \"option2\": \"...\", \"option3\": \"...\", \"option4\": \"...\", \"rightAnswer\": \"...\", \"marks\": 5 }",
                    request.getQuestions(), request.getTopic(), request.getDifficulty()
            );

            // C. Call AI & Save (Reuse your existing logic)
            String jsonResponse = callGeminiApi(prompt);
            List<QuestionDto> dtos = parseToDtoList(jsonResponse);

            // ... (Save Questions Logic) ...
            Quiz finalQuiz = quiz;
            List<Question> questions = dtos.stream().map(dto -> {
                Question q = new Question();
                q.setQuestionTitle(dto.getQuestionTitle());
                q.setOption1(dto.getOption1());
                q.setOption2(dto.getOption2());
                q.setOption3(dto.getOption3());
                q.setOption4(dto.getOption4());
                q.setRightAnswer(dto.getRightAnswer());
                q.setMarks(5);
                q.setQuiz(finalQuiz);
                return q;
            }).collect(Collectors.toList());

            questionRepo.saveAll(questions);
            return quiz.getId();

        } catch (Exception e) {
            throw new RuntimeException("AI Service busy. Please try again later.");
        }
    }
    // --- Helper 1: Normalize Subject (java -> Java) ---
    private String normalizeSubject(String input) {
        // List of your "Core" categories to enforce casing
        List<String> coreList = java.util.Arrays.asList("Java", "Python", "History", "Math", "Science", "General Knowledge");

        for (String core : coreList) {
            if (core.equalsIgnoreCase(input)) {
                return core; // Return the proper Capitalized version (e.g. "Java")
            }
        }
        // If not core, just Title Case it
        return capitalize(input);
    }

    // --- Helper 2: Simple Capitalization ---
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public void checkAndIncrementLimit(String username){
        Customer customer = userRepo.findByUsername(username).orElseThrow(()->new RuntimeException("User not found"));
        LocalDate today = LocalDate.now();

        if (customer.getLastAiUsageDate() == null || !customer.getLastAiUsageDate().equals(today)){
            customer.setAiUsageCount(0);
            customer.setLastAiUsageDate(today);
        }
        if (customer.getAiUsageCount() >= 5 ){
            throw new RuntimeException("Daily AI limit reached (5/5). Please try again tomorrow.");
        }
        customer.setAiUsageCount(customer.getAiUsageCount()+1);
        userRepo.save(customer);
    }

    public Map<String,Object> getUsageStats(String username) {
        Customer user = userRepo.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("User not found"));
        LocalDate date = LocalDate.now();
        int limit = 5;
        int currentUsage = user.getAiUsageCount();
        if(user.getLastAiUsageDate() == null || !user.getLastAiUsageDate().equals(date)){
            currentUsage = 0;
        }
        int left = limit - currentUsage;

        return Map.of(
                "Usage",currentUsage,
                "limit",limit,
                "attemptsLeft",left,
                "isLimitReached",left <= 0
        );
    }
}