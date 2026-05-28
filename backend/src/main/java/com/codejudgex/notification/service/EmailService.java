package com.codejudgex.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text emails via JavaMailSender.
 * In development, routed to MailHog (port 1025) — no credentials needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@codejudgex.local}")
    private String fromAddress;

    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.debug("Email sent — to={} subject={}", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email — to={} subject={}", to, subject, ex);
            // Email failure is non-fatal — notification is already persisted in-app
        }
    }
}
