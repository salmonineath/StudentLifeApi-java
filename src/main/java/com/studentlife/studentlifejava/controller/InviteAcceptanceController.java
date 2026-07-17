package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.service.AssignmentInviteService;
import com.studentlife.studentlifejava.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
@Tag(name = "Assignment Invites", description = "Accept an invited reached from an email link")
@SecurityRequirement(name = "bearerAuth")
public class InviteAcceptanceController {

    private final AssignmentInviteService assignmentInviteService;
    private final AuthUtil authUtil;

    @PostMapping("/{token}/accept")
    @Operation(summary = "Accept an assignment invite by token")
    public ResponseEntity<ApiResponse<Void>> accept(@PathVariable String token) {
        assignmentInviteService.acceptInvite(token, authUtil.getAuthenticatedUser());
        return ResponseEntity.ok(new ApiResponse<>(
                200,
                true,
                "Invite accepted"
        ));
    }
}
