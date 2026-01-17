package com.projectJava.quizApp.service;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.repo.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
@Service
public class CustomerService {

    @Autowired
    private UserRepo userRepo;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Transactional
    public void changePasswordForAuthenticatedUser(String authenticatedUsername,
                                              String oldPassword,
                                              String newPassword){
        Optional<Customer> userOpt = userRepo.findByUsername(authenticatedUsername);
        if(userOpt.isEmpty()){
            throw new IllegalArgumentException("Authenticated user not found");
        }
        Customer user = userOpt.get();
        if(!passwordEncoder.matches(oldPassword,user.getPassword())){
            throw new IllegalArgumentException("Old password is incorrect");
        }
        if(passwordEncoder.matches(newPassword,user.getPassword())){
            throw new IllegalArgumentException("New Password must be different from the old password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepo.save(user);
    }
}
