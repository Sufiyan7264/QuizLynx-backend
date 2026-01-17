package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.VerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationOtpRepo extends JpaRepository<VerificationOtp, Long> {
    Optional<VerificationOtp> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);
    void deleteByEmailAndType(String email, String type);
}
