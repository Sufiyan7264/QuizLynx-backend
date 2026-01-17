package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.StudentDashboardDto;
import com.projectJava.quizApp.service.StudentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/student/dashboard")
public class StudentDashboardController {

    @Autowired
    private StudentDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentDashboardDto> getDashboard(Principal principal) {
        return ResponseEntity.ok(dashboardService.getDashboardData(principal.getName()));
    }
}
