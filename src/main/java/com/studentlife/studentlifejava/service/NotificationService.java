package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.response.NotificationResponse;
import com.studentlife.studentlifejava.entity.Notification;
import com.studentlife.studentlifejava.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    Page<NotificationResponse> getNotifications(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long userId, Long notificationId);

    Notification createNotification(Long userId, String message, NotificationType type);
}
