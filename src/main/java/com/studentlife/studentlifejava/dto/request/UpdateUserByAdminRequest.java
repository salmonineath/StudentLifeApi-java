package com.studentlife.studentlifejava.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserByAdminRequest {

    @NotBlank
    private String reason;
}
