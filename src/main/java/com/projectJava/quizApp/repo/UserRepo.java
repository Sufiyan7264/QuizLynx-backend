package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<Customer,Long> {
    boolean existsByUsername(String username);
    Optional<Customer> findByUsername(String username);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByProviderAndProviderId(String provider, String providerId);

}
