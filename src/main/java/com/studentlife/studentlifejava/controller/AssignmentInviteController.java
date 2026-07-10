package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.request.InviteRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.InviteResponse;
import com.studentlife.studentlifejava.dto.response.MemberResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.service.AssignmentInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments/{assignmentId}")
@RequiredArgsConstructor
@Tag(name = "Assignment Invites", description = "Assignment collaboration invite and member endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AssignmentInviteController {

    private final AssignmentInviteService assignmentInviteService;

    @PostMapping("/invites")
    @Operation(summary = "Invite a user by email (owner/admin only)")
    public ResponseEntity<ApiResponse<InviteResponse>> invite(
            @PathVariable Long assignmentId,
            @Valid @RequestBody InviteRequest request) {
        InviteResponse response = assignmentInviteService.invite(assignmentId, request, currentUser());
        return ResponseEntity.status(201).body(new ApiResponse<>(201, true, "Invite sent.", response));
    }

    @DeleteMapping("/invites/{email}")
    @Operation(summary = "Revoke a pending invite (owner/admin only)")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @PathVariable Long assignmentId,
            @PathVariable String email) {
        assignmentInviteService.revoke(assignmentId, email, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Invite revoked."));
    }

    @GetMapping("/members")
    @Operation(summary = "List the assignment's owner and accepted members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> members(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Members fetched.",
                assignmentInviteService.members(assignmentId, currentUser())));
    }

    private Users currentUser() {
        return (Users) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
