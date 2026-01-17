package com.projectJava.quizApp.service;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.VerificationOtp;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.repo.VerificationOtpRepo;
import com.projectJava.quizApp.utility.OtpUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    @Autowired
    private OtpUtil otpUtil;
    @Autowired
    private VerificationOtpRepo verificationOtpRepo;
//    @Autowired
//    private VerificationOtp verificationOtp;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserRepo userRepo;

    @Value("${otp.max.attempts}")
    private int maxAttempts;
    @Value("${otp.resend.cooldown.seconds}")
    private int otpResendCooldown;

    @Transactional
    public void createAndSendOtpForMail(Long userId, String email,String username) {
        try{
        verificationOtpRepo.deleteByEmailAndType(email, "EMAIL_VERIFICATION");

        String otp = otpUtil.generateOtp();
        String hash = otpUtil.hmacSha256hex(otp);
        LocalDateTime expiresAt = otpUtil.calculateExpiry();
        VerificationOtp verificationOtp=new VerificationOtp();

        verificationOtp.setUserId(userId);
        verificationOtp.setOtpHash(hash);
        verificationOtp.setEmail(email);
        verificationOtp.setExpiresAt(expiresAt);
        verificationOtp.setCreatedAt(LocalDateTime.now());
        verificationOtp.setAttempts(0);
        verificationOtp.setUsed(false);
        verificationOtp.setType("EMAIL_VERIFICATION");
        verificationOtpRepo.save(verificationOtp);
        emailService.sendOtpEMail(email, otp, otpUtil.getOtpExpirationMinutes(),username);
    }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Transactional
    public boolean verifyOtp(String email, String otpSupplied){
        VerificationOtp token = verificationOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(email,"EMAIL_VERIFICATION")
                .orElseThrow(()->new IllegalArgumentException("No Verification Token Found"));

        if(token.isUsed()){
            throw new IllegalStateException("Code Already Used");
        }
        if(token.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new IllegalStateException("Code Expired");
        }
        if(token.getAttempts() >= maxAttempts){
            throw new IllegalStateException("Too many invalid attempts");
        }
        String hashSupplied = otpUtil.hmacSha256hex(otpSupplied);
        boolean ok = otpUtil.constantTimeEquals(hashSupplied,token.getOtpHash());

        if(!ok){
            token.setAttempts(token.getAttempts()+1);
            verificationOtpRepo.save(token);
            throw new IllegalArgumentException("Invalid Code");
        }
        token.setUsed(true);
        verificationOtpRepo.save(token);
        return true;

    }

    public boolean canResend(String email){
        return verificationOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(email,"EMAIL_VERIFICATION")
                .map(token->token.getCreatedAt().plusSeconds(otpResendCooldown).isBefore(LocalDateTime.now())).orElse(false);
    }

    @Transactional
    public void createAndSendPasswordResetOtp(String email) {
        try {
            // Delete previous password reset tokens
            verificationOtpRepo.deleteByEmailAndType(email, "PASSWORD_RESET");

            // Generate a secure token for password reset (URL-safe)
            String otp = otpUtil.generateOtp(); // instead of 6-digit OTP
            String hash = otpUtil.hmacSha256hex(otp);
            LocalDateTime expiresAt = otpUtil.calculateExpiry(); // reuse same expiry logic

            VerificationOtp verificationOtp = new VerificationOtp();
            verificationOtp.setUserId(null); // optional, can fetch user id if needed
            verificationOtp.setOtpHash(hash);
            verificationOtp.setEmail(email);
            verificationOtp.setExpiresAt(expiresAt);
            verificationOtp.setCreatedAt(LocalDateTime.now());
            verificationOtp.setAttempts(0);
            verificationOtp.setUsed(false);
            verificationOtp.setType("PASSWORD_RESET");

            verificationOtpRepo.save(verificationOtp);

            Optional<Customer> usernameOpt = userRepo.findByEmail(email);
            String username = "user";
            if(usernameOpt.isPresent()){
                username = usernameOpt.get().getUsername();
            }
            // Send reset link email instead of numeric OTP
//            String resetLink = appBaseUrl + "/reset-password?token=" + otp + "&email=" + email;
            emailService.sendPasswordResetEmail(email, otp, username, otpUtil.getOtpExpirationMinutes());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    @Transactional
    public boolean verifyPasswordResetOtp(String email, String otp, String newPassword) {
        VerificationOtp token = verificationOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(email, "PASSWORD_RESET")
                .orElseThrow(() -> new IllegalArgumentException("No reset token found"));

        if (token.isUsed()) {
            throw new IllegalStateException("Token already used");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expired");
        }
        if (token.getAttempts() >= maxAttempts) {
            throw new IllegalStateException("Too many invalid attempts");
        }

        String hashSupplied = otpUtil.hmacSha256hex(otp);
        boolean ok = otpUtil.constantTimeEquals(hashSupplied, token.getOtpHash());

        if (!ok) {
            token.setAttempts(token.getAttempts() + 1);
            verificationOtpRepo.save(token);
            throw new IllegalArgumentException("Invalid token");
        }

        // token is valid -> proceed to reset password
        Optional<Customer> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            // defensive: user not found (shouldn't happen if we created token for existing email)
            throw new IllegalStateException("No user found for provided email");
        }
        Customer user = userOpt.get();


        // set encoded password and update passwordChangedAt (so old JWTs are invalidated)
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(java.time.Instant.now());
        userRepo.save(user);

        // mark this token used and remove other password-reset tokens for this email
        token.setUsed(true);
        verificationOtpRepo.save(token);
        verificationOtpRepo.deleteByEmailAndType(email, "PASSWORD_RESET");

        // notify user about password change
//        emailService.sendPasswordChangedNotification(email);
        return true;
    }

    public boolean canResendPasswordReset(String email) {
        return verificationOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(email, "PASSWORD_RESET")
                .map(token -> token.getCreatedAt().plusSeconds(otpResendCooldown).isBefore(LocalDateTime.now()))
                .orElse(false);
    }

}
