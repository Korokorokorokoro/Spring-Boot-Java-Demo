package com.springbootjavatest.demo.service;

import com.springbootjavatest.demo.exception.NotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

// Service for sending email notifications.
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;

    @Value("${app.notification.email}")
    private String notificationEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Retryable(
        retryFor = {MailException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 1.5)
    )
    public void sendNotification(String subject, String body) {
        try {
            logger.debug("Attempting to send email notification to: {} with subject: {}", notificationEmail, subject);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notificationEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            
            logger.info("Email notification sent successfully to: {} with subject: {}", notificationEmail, subject);
        } catch (Exception e) {
            logger.error("Failed to send email notification to: {} with subject: {}. Error: {}", 
                    notificationEmail, subject, e.getMessage(), e);
            throw new NotificationException("Failed to send email notification", e);
        }
    }

    @Recover
    public void recoverEmailNotification(MailException e, String subject, String body) {
        logger.error("Failed to send email after all retry attempts. Subject: {}, Error: {}", subject, e.getMessage());
        throw new NotificationException("Email failed after 3 attempts: " + e.getMessage(), e);
    }
}