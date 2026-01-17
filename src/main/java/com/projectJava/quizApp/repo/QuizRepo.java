package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.enums.QuizStatus;
import com.projectJava.quizApp.model.QResult;
import com.projectJava.quizApp.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepo extends JpaRepository<Quiz, Long> {
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.batch.instructor.customer.username = :username")
    long countByInstructor(@Param("username") String username);
    // Find all quizzes assigned to a specific batch
    List<Quiz> findByBatchId(Long batchId);
    // 2. NEW: Count quizzes (Fixes your error)
    long countByBatchId(Long batchId);

    @Query("SELECT q FROM Quiz q WHERE q.batch.instructor.customer.id = :userId")
    List<Quiz> findAllByInstructorId(Long userId);
    @Query("SELECT q FROM Quiz q WHERE q.batch.id = :batchId " +
            "AND q.status = 'PUBLISHED' " +
            "AND q.startDate <= CURRENT_TIMESTAMP " +
            "AND q.endDate >= CURRENT_TIMESTAMP")
    List<Quiz> findActiveQuizzesForStudent(Long batchId);
//    List<QResult> findByQuizId(Long quizId);
List<Quiz> findByTitleStartingWithAndBatchIsNull(String titlePrefix);    // Fetch all Published quizzes (ignoring drafts)
    Page<Quiz> findByStatus(QuizStatus status, Pageable pageable);
    Optional<Quiz> findTopByTitleAndBatchIsNull(String title);
    // Fetch unique subjects (categories)
    @Query("SELECT DISTINCT q.subject FROM Quiz q WHERE q.status = 'PUBLISHED'")
    List<String> findDistinctSubjects();
    // NEW: Count quizzes that don't belong to any class (Public Quizzes)
    long countByBatchIsNull();

    // Also useful for your Explore Page later:
    List<Quiz> findByBatchIsNull();

    @Query("SELECT q FROM Quiz q WHERE q.batch.instructor.customer.id = :userId AND q.id = :id")
    Quiz findAllByInstructorIdAndQuizId(Long userId, Long id);

    @Query("SELECT q FROM Quiz q WHERE q.batch.id IN :batchIds AND q.status = 'PUBLISHED'")
    List<Quiz> findActiveQuizzesForBatches(@Param("batchIds") List<Long> batchIds);
    // 3. NEW: Delete quizzes (Required for the delete logic)
    void deleteByBatchId(Long batchId);

    // FIX 1: Add this for counting quizzes in getCategories()
    long countBySubjectIgnoringCase(String subject);

    // FIX 2: Add this for fetching the category library page
    List<Quiz> findBySubjectIgnoreCaseAndBatchIsNull(String subject);

}
