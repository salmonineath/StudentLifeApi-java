package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.NotificationResponse;
import com.studentlife.studentlifejava.dto.response.UnreadCountResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.exception.ApiException;
import com.studentlife.studentlifejava.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/notifications", "/api/v1/notification"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal Users user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(resolveUserId(user), pageable);
        return ResponseEntity.ok(new ApiResponse<>(
                200,
                true,
                "Notifications fetched successfully.",
                notifications
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(@AuthenticationPrincipal Users user) {
        UnreadCountResponse response = new UnreadCountResponse(notificationService.getUnreadCount(resolveUserId(user)));
        return ResponseEntity.ok(new ApiResponse<>(
                200,
                true,
                "Unread notification count fetched successfully.",
                response
        ));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal Users user,
            @PathVariable Long id
    ) {
        NotificationResponse notification = notificationService.markAsRead(resolveUserId(user), id);
        return ResponseEntity.ok(new ApiResponse<>(
                200,
                true,
                "Notification marked as read.",
                notification
        ));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal Users user) {
        notificationService.markAllAsRead(resolveUserId(user));
        return ResponseEntity.ok(new ApiResponse<>(200, true, "All notifications marked as read."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal Users user,
            @PathVariable Long id
    ) {
        notificationService.deleteNotification(resolveUserId(user), id);
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Notification deleted successfully."));
    }

    private Long resolveUserId(Users user) {
        if (user == null || user.getId() == null) {
            throw new ApiException(401, "Authentication required.");
        }
        return user.getId();
    }
}
