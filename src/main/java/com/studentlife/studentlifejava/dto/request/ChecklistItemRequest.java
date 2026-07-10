package com.studentlife.studentlifejava.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChecklistItemRequest {

    @NotBlank(message = "Text is required")
    private String text;
}
