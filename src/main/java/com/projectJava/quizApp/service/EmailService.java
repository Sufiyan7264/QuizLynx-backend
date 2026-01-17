package com.projectJava.quizApp.service;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.from}")
    private String from;

//    @Autowired
    @Async
    public void sendOtpEMail(String to, String otp, int minutesValid,String username){
     try {
         String subject = "Your Verification Code";
         MimeMessage message = mailSender.createMimeMessage();
         MimeMessageHelper helper = new MimeMessageHelper(message,true);
         helper.setFrom(new InternetAddress(from, "QuizLynx"));

//        String text = String.format("Your verification code is %s. It expires in %d minutes. If you did not request this, ignore this email.", otp, minutesValid);
//        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();

         String html = """
                <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                  <h2 style="color: #007bff;  text-align:center;">QuizLynx Verification</h2>
                  <p>Dear %s,</p>
                  <p>Your one-time verification code is:</p>
                  <h1 style="color: #28a745; letter-spacing: 3px; text-align:center;">%s</h1>
                  <p>This code will expire in <b>%d minutes</b>.</p>
                  <p style="font-size: 12px; color: #777;">
                    If you did not request this, please ignore this email.
                  </p>
                  <hr>
                  <p style="font-size: 12px; color: #555;">© 2025 QuizLynx. All rights reserved.</p>
                </div>
                """.formatted(username,otp, minutesValid);

         helper.setTo(new InternetAddress(to, username));
         helper.setSubject(subject);
         helper.setText(html,true);

         mailSender.send(message);
         System.out.println("OTP email sent to {}"+ to);

     }
     catch (Exception e ){
         throw new RuntimeException("Failed to send OTP email", e);
     }
    }

    @Async
    public void sendPasswordResetEmail(String email, String otp, String username, int otpExpirationMinutes) {
        try {
            String subject = "Your Verification Code";
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true);
            helper.setFrom(new InternetAddress(from, "QuizLynx"));

//        String text = String.format("Your verification code is %s. It expires in %d minutes. If you did not request this, ignore this email.", otp, minutesValid);
//        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();

            String html = """
                <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                  <h2 style="color: #007bff;  text-align:center;">QuizLynx Verification</h2>
                  <p>Dear %s,</p>
                  <p>Your one-time verification code for reset password is :</p>
                  <h1 style="color: #28a745; letter-spacing: 3px; text-align:center;">%s</h1>
                  <p>This code will expire in <b>%d minutes</b>.</p>
                  <p style="font-size: 12px; color: #777;">
                    If you did not request this, please ignore this email.
                  </p>
                  <hr>
                  <p style="font-size: 12px; color: #555;">© 2025 QuizLynx. All rights reserved.</p>
                </div>
                """.formatted(username,otp, otpExpirationMinutes);

            helper.setTo(new InternetAddress(email, username));
            helper.setSubject(subject);
            helper.setText(html,true);

            mailSender.send(message);
            System.out.println("OTP email sent to {}"+ email);

        }
        catch (Exception e ){
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}
