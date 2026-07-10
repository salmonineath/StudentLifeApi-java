package com.studentlife.studentlifejava.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class AssignmentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String subject;

    private String description;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private Instant dueDate;

    private Long courseId;
}
