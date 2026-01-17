package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSubmissionRepo extends JpaRepository<QuizSubmission, Long> {

    // Used to find all attempts by a specific student for a specific quiz
    List<QuizSubmission> findByQuizIdAndStudentId(Long quizId, String studentId);

    // If you ever need to find just the very last one directly via SQL (Optional optimization)
    // QuizSubmission findTopByQuizIdAndStudentIdOrderBySubmitTimeDesc(Long quizId, String studentId);
}