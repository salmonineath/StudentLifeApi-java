package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.CurrentUserResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.service.UserService;
import com.studentlife.studentlifejava.utils.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthUtil authUtil;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getMe() {
        Users currentUser = authUtil.getAuthenticatedUser();
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        true,
                        "Current user fetched.",
                        userService.getCurrentUser(currentUser)
                )
        );
    }
}
