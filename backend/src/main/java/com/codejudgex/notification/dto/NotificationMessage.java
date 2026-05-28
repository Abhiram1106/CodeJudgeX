package com.codejudgex.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * RabbitMQ message published to notification.queue.
 * Produced by EvaluationWorker, ContestService, etc.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private UUID   recipientId;
    private String title;
    private String body;
    private String recipientEmail;
    private boolean sendEmail;
}
