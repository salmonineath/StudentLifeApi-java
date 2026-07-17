package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.request.ChecklistItemRequest;
import com.studentlife.studentlifejava.dto.request.ChecklistItemToggleRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.ChecklistItemResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.service.ChecklistItemService;
import com.studentlife.studentlifejava.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Checklist", description = "Task checklist item endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ChecklistItemController {

    private final ChecklistItemService checklistItemService;
    private final AuthUtil authUtil;

    @PostMapping("/tasks/{taskId}/checklist")
    @Operation(summary = "Add a checklist item to a task")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> create(
            @PathVariable Long taskId,
            @Valid @RequestBody ChecklistItemRequest request) {
        ChecklistItemResponse response = checklistItemService.create(taskId, request, currentUser());
        return ResponseEntity.status(201).body(new ApiResponse<>(201, true, "Checklist item created.", response));
    }

    @PatchMapping("/checklist/{itemId}/toggle")
    @Operation(summary = "Set a checklist item's done state")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> toggle(
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemToggleRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Checklist item updated.",
                checklistItemService.toggle(itemId, request, currentUser())));
    }

    @DeleteMapping("/checklist/{itemId}")
    @Operation(summary = "Remove a checklist item")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long itemId) {
        checklistItemService.delete(itemId, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Checklist item deleted."));
    }

    // Delegates to AuthUtil instead of casting the raw principal: the inline
    // (Users) cast blows up with a 500 on a null or anonymous authentication,
    // where AuthUtil throws a proper 401.
    private Users currentUser() {
        return authUtil.getAuthenticatedUser();
    }
}
