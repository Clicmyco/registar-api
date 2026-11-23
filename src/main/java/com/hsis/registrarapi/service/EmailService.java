package com.hsis.registrarapi.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // ObjectProvider lets the app start cleanly even if SMTP isn't configured yet
    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        if (mailSender != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("HSIS Portal — Password Reset Confirmation Code");
            message.setText(
                "Hello,\n\n" +
                "Your password reset confirmation code is: " + code + "\n\n" +
                "This code will expire in 15 minutes. If you did not request a password reset, you can safely ignore this email."
            );
            mailSender.send(message);
        } else {
            // Dev fallback when SMTP credentials aren't set in application.properties
            System.out.println("==========================================");
            System.out.println("PASSWORD RESET CODE FOR " + toEmail + ": " + code);
            System.out.println("==========================================");
        }
    }
}