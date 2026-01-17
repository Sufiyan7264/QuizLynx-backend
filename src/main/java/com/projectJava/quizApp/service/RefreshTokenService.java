package com.projectJava.quizApp.service;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.RefreshToken;
import com.projectJava.quizApp.repo.RefreshTokenRepo;
import com.projectJava.quizApp.repo.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${jwt.refresh.expiration:604800000}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;
    @Autowired
    private UserRepo userRepo;

    public RefreshToken createRefreshToken(String username) {
        // 1. Fetch the User
        Customer user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Check if this user ALREADY has a token
        RefreshToken refreshToken = refreshTokenRepo.findByCustomer(user)
                .orElse(new RefreshToken()); // If not found, create a new object

        // 3. Set/Update the fields
        refreshToken.setCustomer(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs)); // e.g., 7 days
        refreshToken.setToken(UUID.randomUUID().toString());

        // 4. Save (Spring Data will perform UPDATE if ID exists, or INSERT if ID is null)
        return refreshTokenRepo.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token){
        if(token.getExpiryDate().compareTo(Instant.now())<0){
            refreshTokenRepo.delete(token);
            throw new RuntimeException("Refresh token was expired, Please sign in again");
        }
        return token;
    }
    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenStr){
        RefreshToken oldToken = refreshTokenRepo.findByToken(oldTokenStr).
                orElseThrow(()-> new RuntimeException("Refresh token is not available , please try sign in again"));
        verifyExpiration(oldToken);
        refreshTokenRepo.delete(oldToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setCustomer(oldToken.getCustomer());
        return refreshTokenRepo.save(refreshToken);

    }

    @Transactional
    public void deleteByUserId(Long id){
        Customer c = userRepo.findById(id).get();
        refreshTokenRepo.deleteByCustomer(c);
    }
}
