package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.response.NotificationResponse;
import com.studentlife.studentlifejava.entity.Notification;
import com.studentlife.studentlifejava.entity.NotificationType;
import com.studentlife.studentlifejava.exception.NotificationNotFoundException;
import com.studentlife.studentlifejava.repository.NotificationRepository;
import com.studentlife.studentlifejava.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        long deleted = notificationRepository.deleteByIdAndUserId(notificationId, userId);
        if (deleted == 0) {
            throw new NotificationNotFoundException(notificationId);
        }
    }

    @Override
    @Transactional
    public Notification createNotification(Long userId, String message, NotificationType type) {
        Notification notification = new Notification(userId, message, type);
        return notificationRepository.save(notification);
    }
}
