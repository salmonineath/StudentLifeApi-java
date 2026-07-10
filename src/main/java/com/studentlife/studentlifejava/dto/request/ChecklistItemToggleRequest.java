package com.studentlife.studentlifejava.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChecklistItemToggleRequest {

    @NotNull(message = "done is required")
    private Boolean done;
}
