package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.response.NotificationResponse;
import com.studentlife.studentlifejava.dto.response.PageResponse;
import com.studentlife.studentlifejava.entity.Notification;
import com.studentlife.studentlifejava.entity.NotificationType;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.NotificationRepository;
import com.studentlife.studentlifejava.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.notFound;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public PageResponse<NotificationResponse> list(Users currentUser, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByUserOrderByCreatedAtDesc(currentUser, pageable)
                .map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public long unreadCount(Users currentUser) {
        return notificationRepository.countByUserAndIsReadFalse(currentUser);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Users currentUser) {
        Notification notification = findOwned(notificationId, currentUser);
        notification.setIsRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(Users currentUser) {
        notificationRepository.markAllAsReadForUser(currentUser);
    }

    @Override
    @Transactional
    public void delete(Long notificationId, Users currentUser) {
        Notification notification = findOwned(notificationId, currentUser);
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void create(Users user, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    // Looks up by (id, user) rather than id alone so a user can never read,
    // toggle, or delete another user's notification via a guessed id.
    private Notification findOwned(Long notificationId, Users currentUser) {
        return notificationRepository.findByIdAndUser(notificationId, currentUser)
                .orElseThrow(() -> notFound("Notification not found."));
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
