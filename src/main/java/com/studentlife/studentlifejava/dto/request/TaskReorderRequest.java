package com.studentlife.studentlifejava.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TaskReorderRequest {

    @NotEmpty(message = "orderedIds must not be empty")
    private List<Long> orderedIds;
}
