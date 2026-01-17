package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnnouncementRepo extends JpaRepository<Announcement, Long> {
    // Find announcements for a list of batch IDs (the batches the student is in)
    @Query("SELECT a FROM Announcement a WHERE a.batch.id IN :batchIds ORDER BY a.date DESC")
    List<Announcement> findByBatchIdIn(List<Long> batchIds);
}
