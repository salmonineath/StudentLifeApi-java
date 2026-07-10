package com.studentlife.studentlifejava.dto.response;

import com.studentlife.studentlifejava.entity.InviteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteResponse {

    private Long id;
    private Long assignmentId;
    private String email;
    private InviteStatus status;
    private Instant createdAt;
}
