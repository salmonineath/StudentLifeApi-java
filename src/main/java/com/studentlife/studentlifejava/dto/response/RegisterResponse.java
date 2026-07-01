package com.studentlife.studentlifejava.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private String accessToken;

    private UserResponse user;
}
