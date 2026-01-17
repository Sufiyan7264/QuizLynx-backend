package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.DTO.LeaderboardDto;
import com.projectJava.quizApp.model.QResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QResultRepo extends JpaRepository<QResult, Long> {
    // Change from Optional<QResult> to List<QResult>
    List<QResult> findByStudentIdAndQuizIdAndSubmitDateIsNull(Long studentId, Long quizId);    List<QResult> findByStudentId(Long studentId);
    // Fetch all results for quizzes created by this instructor
    @Query("SELECT r FROM QResult r WHERE r.quiz.batch.instructor.customer.username = :username")
    List<QResult> findAllByInstructor(@Param("username") String username);


    // Fetch results from last 7 days for engagement chart
    @Query("SELECT r FROM QResult r WHERE r.quiz.batch.instructor.customer.username = :username " +
            "AND r.submitDate >= :sevenDaysAgo")
    List<QResult> findRecentResultsByInstructor(@Param("username") String username,
                                                @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo);
    @Query("SELECT q.quiz.id FROM QResult q WHERE q.student.username = :username")
    List<Long> findAttemptedQuizIdsByStudent(@Param("username") String username);
    // 2. Find all results for a specific quiz (For Instructor "Class Performance" view)
    List<QResult> findByQuizId(Long quizId);
    List<QResult> findByStudent_UsernameOrderBySubmitDateDesc(String username);
    Page<QResult> findByStudent_UsernameAndSubmitDateIsNotNullOrderBySubmitDateDesc(String username, Pageable pageable);
    @Query("SELECT r FROM QResult r WHERE r.student.id = :studentId AND r.quiz.batch.id = :batchId")
    List<QResult> findByStudentIdAndBatchId(@Param("studentId") Long studentId, @Param("batchId") Long batchId);
    // Count how many results exist for this quiz
    long countByQuizId(Long quizId);
    @Query("SELECT new com.projectJava.quizApp.DTO.LeaderboardDto(" +
            "   q.student.username, " + // Name
            "   SUM(q.scoreObtained), " + // Score
            "   q.student.country " +     // Country (if you have it)
            ") " +
            "FROM QResult q " +
            "WHERE q.student.role = 'USER' " + // Filter for individual users
            "AND q.quiz.batch IS NULL " +      // Filter for public quizzes only
            "GROUP BY q.student.username, q.student.country " +
            "ORDER BY SUM(q.scoreObtained) DESC")
    List<LeaderboardDto> findTopRankedScores();

    // Check if any student submitted an answer for a specific question
    @Query("SELECT COUNT(sa) > 0 FROM SubmittedAnswer sa WHERE sa.question.id = :questionId")
    boolean existsByQuestionId(@Param("questionId") Long questionId);
}