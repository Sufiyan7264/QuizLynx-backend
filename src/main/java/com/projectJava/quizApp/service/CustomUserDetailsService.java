package com.projectJava.quizApp.service;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service

public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private OtpService otpService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer user = userRepo.findByUsername(username)
                .orElseThrow();
        if(!user.getVerified()){
            otpService.createAndSendOtpForMail(user.getId(),user.getEmail(),user.getUsername());
            throw new RuntimeException("User is not verified. Please verify your account using the OTP sent to your email.");
        }
        String password = user.getPassword();
        if (password == null || password.isEmpty()) {
                password = "OAUTH2_USER_PLACEHOLDER";
            }

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    password,
                    Collections.singleton(new SimpleGrantedAuthority(user.getRole()))
            );
    }
}

