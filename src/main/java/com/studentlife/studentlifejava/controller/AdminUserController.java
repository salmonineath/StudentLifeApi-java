package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.request.UpdateUserByAdminRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.CurrentUserResponse;
import com.studentlife.studentlifejava.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CurrentUserResponse>>> getAllUsers() {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        true,
                        "User fetched",
                        userService.getAllUser()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        true,
                        "User fetched",
                        userService.getUserById(id)
                )
        );
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUserByAdminRequest request) {
        userService.banUser(id, request);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        true,
                        "User banned."
                )
        );
    }

    @PatchMapping("/{id}/unban")
    public ResponseEntity<ApiResponse<Void>> unBanUser(@PathVariable Long id) {
        userService.unBanUser(id);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        true,
                        "User unbanned"
                )
        );
    }
}
