package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.UserDashboardDto;
import com.projectJava.quizApp.service.UserDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/users/dashboard")
public class UserDashboardController {

    @Autowired
    private UserDashboardService dashboardService;

    // Use 'hasRole('USER')' or whatever role your individual users have
    // If they are just generic users, you might use 'isAuthenticated()'
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','STUDENT')")
    public ResponseEntity<UserDashboardDto> getDashboard(Principal principal) {
        return ResponseEntity.ok(dashboardService.getUserDashboardData(principal.getName()));
    }
}