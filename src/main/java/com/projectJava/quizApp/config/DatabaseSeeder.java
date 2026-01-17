package com.projectJava.quizApp.config;

import com.projectJava.quizApp.repo.QuizRepo;
import com.projectJava.quizApp.service.QuizGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private QuizRepo quizRepo;

    @Autowired
    private QuizGeneratorService generatorService;

    @Override
    public void run(String... args) throws Exception {

        // CORRECTION: Only check for quizzes that have NO Batch (Public Quizzes)
        // You need to add this method to your QuizRepo: long countByBatchIsNull();
        long publicQuizCount = quizRepo.countByBatchIsNull();

        if (publicQuizCount > 0) {
            System.out.println("Global/Public quizzes already exist (" + publicQuizCount + "). Skipping seed.");
            return;
        }

        System.out.println("No public quizzes found. Seeding initial library from OpenTDB...");

        // Define Categories & Difficulties
        String[] categories = {"Computer Science", "Mathematics", "History", "Science", "General Knowledge"};
        String[] difficulties = {"easy", "medium"}; // Skip hard for initial seed to keep it friendly

        int count = 0;
        for (String cat : categories) {
            for (String diff : difficulties) {
                // Generate 3 quizzes per category/difficulty combo
                for (int i = 0; i < 3; i++) {
                    try {
                        System.out.println("Generating: " + diff + " " + cat + "...");

                        // The generator service handles saving them with batch=null
                        generatorService.generateInstantQuiz(cat, diff);
                        System.out.println("Waiting 5 seconds for rate limit...");
                        Thread.sleep(5000);
                        count++;
                    } catch (Exception e) {
                        System.err.println("Skipping " + cat + ": " + e.getMessage());
                    }
                }
            }
        }

        System.out.println("Seeding Complete! Added " + count + " public quizzes.");
    }
}