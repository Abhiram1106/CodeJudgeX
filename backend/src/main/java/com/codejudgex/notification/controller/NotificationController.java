package com.codejudgex.notification.controller;

import com.codejudgex.common.dto.ApiResponse;
import com.codejudgex.common.dto.PageResponse;
import com.codejudgex.common.exception.ResourceNotFoundException;
import com.codejudgex.notification.dto.NotificationResponse;
import com.codejudgex.notification.entity.Notification;
import com.codejudgex.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification inbox")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List notifications for the authenticated user (newest first)")
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        Page<NotificationResponse> mapped = notifications.map(n -> NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build());

        return ApiResponse.success(PageResponse.<NotificationResponse>builder()
                .content(mapped.getContent())
                .page(mapped.getNumber())
                .size(mapped.getSize())
                .totalElements(mapped.getTotalElements())
                .totalPages(mapped.getTotalPages())
                .last(mapped.isLast())
                .build());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<Void> markRead(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        int updated = notificationRepository.markAsRead(id, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
        return ApiResponse.success(null, "Marked as read");
    }
}
