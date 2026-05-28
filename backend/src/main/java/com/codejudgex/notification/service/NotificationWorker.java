package com.codejudgex.notification.service;

import com.codejudgex.infrastructure.config.RabbitMQConfig;
import com.codejudgex.notification.dto.NotificationMessage;
import com.codejudgex.notification.entity.Notification;
import com.codejudgex.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes NotificationMessage from notification.queue.
 * Persists an in-app notification and optionally sends email via MailHog.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationRepository notificationRepository;
    private final EmailService           emailService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onNotificationMessage(NotificationMessage message) {
        log.debug("Notification received — recipient={} title={}", message.getRecipientId(), message.getTitle());

        Notification notification = new Notification();
        notification.setUserId(message.getRecipientId());
        notification.setTitle(message.getTitle());
        notification.setMessage(message.getBody());
        notificationRepository.save(notification);

        if (message.isSendEmail() && message.getRecipientEmail() != null && !message.getRecipientEmail().isBlank()) {
            emailService.send(message.getRecipientEmail(), message.getTitle(), message.getBody());
        }
    }
}
