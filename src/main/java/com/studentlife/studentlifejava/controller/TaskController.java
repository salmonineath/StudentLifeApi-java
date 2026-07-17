package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.request.TaskReorderRequest;
import com.studentlife.studentlifejava.dto.request.TaskRequest;
import com.studentlife.studentlifejava.dto.request.TaskStatusUpdateRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.TaskResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.service.TaskService;
import com.studentlife.studentlifejava.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Assignment task endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;
    private final AuthUtil authUtil;

    @PostMapping("/assignments/{assignmentId}/tasks")
    @Operation(summary = "Add a task to an assignment")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @PathVariable Long assignmentId,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.create(assignmentId, request, currentUser());
        return ResponseEntity.status(201).body(new ApiResponse<>(201, true, "Task created.", response));
    }

    @PatchMapping("/assignments/{assignmentId}/tasks/reorder")
    @Operation(summary = "Reorder tasks within an assignment")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> reorder(
            @PathVariable Long assignmentId,
            @Valid @RequestBody TaskReorderRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Tasks reordered.",
                taskService.reorder(assignmentId, request, currentUser())));
    }

    @PutMapping("/tasks/{taskId}")
    @Operation(summary = "Update a task's title, description, or assignees")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Task updated.",
                taskService.update(taskId, request, currentUser())));
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long taskId) {
        taskService.delete(taskId, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Task deleted."));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @Operation(summary = "Set a task's status (todo/progress/done)")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Task status updated.",
                taskService.updateStatus(taskId, request, currentUser())));
    }

    // Delegates to AuthUtil instead of casting the raw principal: the inline
    // (Users) cast blows up with a 500 on a null or anonymous authentication,
    // where AuthUtil throws a proper 401.
    private Users currentUser() {
        return authUtil.getAuthenticatedUser();
    }
}
