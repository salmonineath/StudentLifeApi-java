package com.studentlife.studentlifejava.dto.response;

import com.studentlife.studentlifejava.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private Instant createdAt;
}
