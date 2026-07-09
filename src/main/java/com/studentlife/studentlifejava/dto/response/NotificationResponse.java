package com.studentlife.studentlifejava.dto.response;

import com.studentlife.studentlifejava.entity.Notification;
import com.studentlife.studentlifejava.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String message,
        NotificationType type,
        boolean isRead,
        Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
