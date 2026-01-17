package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.ContactRequestDto;
import com.projectJava.quizApp.utility.EmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String adminEmail;

    @Async
    public void sendContactEmail(ContactRequestDto dto) {
        try {
            // 1. Create a MimeMessage (Required for HTML)
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // 2. Set Email Details
            helper.setTo(adminEmail);

            // Helpful: Set the "Reply-To" so when you click Reply in Gmail, it goes to the user, not yourself
            helper.setReplyTo(dto.getEmail());

            String subject = dto.getSubject() != null && !dto.getSubject().isEmpty()
                    ? dto.getSubject()
                    : "New Inquiry from Quizlynx";
            helper.setSubject("🔔 " + subject); // Add emoji for visibility

            // 3. Generate HTML Content
            String htmlContent = EmailTemplate.getContactTemplate(
                    dto.getName(),
                    dto.getEmail(),
                    subject,
                    dto.getMessage()
            );

            // 4. Set Content (true = isHtml)
            helper.setText(htmlContent, true);

            // 5. Send
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }
}