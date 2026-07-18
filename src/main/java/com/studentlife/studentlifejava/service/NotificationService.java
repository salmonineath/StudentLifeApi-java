package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.response.NotificationResponse;
import com.studentlife.studentlifejava.dto.response.PageResponse;
import com.studentlife.studentlifejava.entity.NotificationType;
import com.studentlife.studentlifejava.entity.Users;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    PageResponse<NotificationResponse> list(Users currentUser, Pageable pageable);

    long unreadCount(Users currentUser);

    NotificationResponse markAsRead(Long notificationId, Users currentUser);

    void markAllAsRead(Users currentUser);

    void delete(Long notificationId, Users currentUser);

    // Called by other services (invites, deadlines, chat, etc.) to raise a
    // notification for a user - not exposed over HTTP.
    void create(Users user, String message, NotificationType type);
}
