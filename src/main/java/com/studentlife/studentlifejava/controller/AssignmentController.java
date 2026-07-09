package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.request.AssignmentRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.AssignmentResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Tag(name = "Assignments", description = "Assignment tracking endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @Operation(summary = "Create a new assignment")
    public ResponseEntity<ApiResponse<AssignmentResponse>> create(
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse response = assignmentService.create(request, currentUser());
        return ResponseEntity.status(201).body(new ApiResponse<>(201, true, "Assignment created.", response));
    }

    @GetMapping
    @Operation(summary = "List all assignments for the authenticated user (paginated)")
    public ResponseEntity<ApiResponse<Page<AssignmentResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AssignmentResponse> result = assignmentService.list(
                currentUser(), PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Assignments fetched.", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single assignment by ID")
    public ResponseEntity<ApiResponse<AssignmentResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Assignment fetched.",
                assignmentService.get(id, currentUser())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an assignment")
    public ResponseEntity<ApiResponse<AssignmentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Assignment updated.",
                assignmentService.update(id, request, currentUser())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an assignment (owner or admin only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        assignmentService.delete(id, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Assignment deleted."));
    }

    private Users currentUser() {
        return (Users) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
