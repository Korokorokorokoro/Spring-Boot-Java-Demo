package com.springbootjavatest.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Service for sending SMS notifications.
@Service
public class SmsService {

    @Value("${app.notification.sms}")
    private String notificationSms;

    public void sendNotification(String subject, String body) {
        // Mock SMS sending - in real app, integrate with Twilio, AWS SNS, etc.
        System.out.println("SMS to " + notificationSms + ": " + subject + " - " + body);
    }
}