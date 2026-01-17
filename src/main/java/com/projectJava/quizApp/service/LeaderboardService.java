package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.LeaderboardDto;
import com.projectJava.quizApp.model.QResult;
import com.projectJava.quizApp.repo.QResultRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class LeaderboardService {


    @Autowired
    private QResultRepo qResultRepo;
    public List<LeaderboardDto> getLeaderboardData() {
        List<LeaderboardDto> leaderboard = qResultRepo.findTopRankedScores();

        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }

        return leaderboard;
    }
}
