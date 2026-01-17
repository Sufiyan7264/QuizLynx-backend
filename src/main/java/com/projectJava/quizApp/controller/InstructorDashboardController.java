package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.DashboardResponseDto;
import com.projectJava.quizApp.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/instructor/dashboard")
public class InstructorDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<DashboardResponseDto> getDashboardStats(Principal principal) {
        return ResponseEntity.ok(dashboardService.getDashboardData(principal.getName()));
    }
}