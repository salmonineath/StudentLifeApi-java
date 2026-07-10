package com.studentlife.studentlifejava.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDetailResponse {

    private Long id;
    private String title;
    private String subject;
    private String description;
    private Instant dueDate;
    private Long courseId;
    private Integer progress;
    private Boolean completed;
    private Long createdById;
    private List<TaskResponse> tasks;
    private List<String> invites;
    private Instant createdAt;
    private Instant updatedAt;
}
