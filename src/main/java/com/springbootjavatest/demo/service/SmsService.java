package com.springbootjavatest.demo.service;

import com.springbootjavatest.demo.exception.NotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

// Service for sending SMS notifications.
@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    
    @Value("${app.notification.sms}")
    private String notificationSms;

    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 1.5)
    )
    public void sendNotification(String subject, String body) {
        try {
            logger.debug("Attempting to send SMS notification to: {} with subject: {}", notificationSms, subject);
            
            // Mock SMS sending - in real app, integrate with Twilio, AWS SNS, etc.
            System.out.println("SMS to " + notificationSms + ": " + subject + " - " + body);
            
            logger.info("SMS notification sent successfully to: {} with subject: {}", notificationSms, subject);
        } catch (Exception e) {
            logger.error("Failed to send SMS notification to: {} with subject: {}. Error: {}", 
                    notificationSms, subject, e.getMessage(), e);
            throw new NotificationException("Failed to send SMS notification", e);
        }
    }

    @Recover
    public void recoverSmsNotification(RuntimeException e, String subject, String body) {
        logger.error("Failed to send SMS after all retry attempts. Subject: {}, Error: {}", subject, e.getMessage());
        throw new NotificationException("SMS failed after 3 attempts: " + e.getMessage(), e);
    }
}