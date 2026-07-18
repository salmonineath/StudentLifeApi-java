package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.NotificationResponse;
import com.studentlife.studentlifejava.dto.response.PageResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.service.NotificationService;
import com.studentlife.studentlifejava.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthUtil authUtil;

    @GetMapping
    @Operation(summary = "List notifications for the current user")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Notifications retrieved.",
                notificationService.list(currentUser(), pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get the count of unread notifications for the current user")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Unread count retrieved.",
                notificationService.unreadCount(currentUser())));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Notification marked as read.",
                notificationService.markAsRead(id, currentUser())));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead(currentUser());
        return ResponseEntity.ok(new ApiResponse<>(200, true, "All notifications marked as read."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.delete(id, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Notification deleted."));
    }

    // Delegates to AuthUtil instead of casting the raw principal: the inline
    // (Users) cast blows up with a 500 on a null or anonymous authentication,
    // where AuthUtil throws a proper 401.
    private Users currentUser() {
        return authUtil.getAuthenticatedUser();
    }
}
