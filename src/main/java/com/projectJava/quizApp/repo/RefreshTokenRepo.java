package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByCustomer(Customer customer); // For Logout
    Optional<RefreshToken> findByCustomer(Customer user);
}