package com.studentlife.studentlifejava.dto.response;

import com.studentlife.studentlifejava.entity.TaskStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TaskResponse {

    private Long id;
    private Long assignmentId;
    private String title;
    private String description;
    private TaskStatus status;
    private List<Long> assigneeIds;
    private List<ChecklistItemResponse> checklist;
    private List<AttachmentResponse> attachments;

    @JsonProperty("order")
    private Integer sortOrder;

    private Instant createdAt;
    private Instant updatedAt;
}
