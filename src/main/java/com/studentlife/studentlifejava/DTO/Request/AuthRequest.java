package com.studentlife.studentlifejava.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "Email or username is required")
    private String email_or_username;

    @NotBlank(message = "Password is required")
    private String password;
}
