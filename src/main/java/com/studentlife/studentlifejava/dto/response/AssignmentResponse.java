package com.studentlife.studentlifejava.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {

    private Long id;
    private String title;
    private String description;
    private Instant dueDate;
    private Long courseId;
    private Long createdById;
    private Instant createdAt;
    private Instant updatedAt;
}
