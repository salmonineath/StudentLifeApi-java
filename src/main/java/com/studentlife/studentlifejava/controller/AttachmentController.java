package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.AttachmentResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Task file attachment endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = "multipart/form-data")
    @Operation(summary = "Upload a file attachment to a task")
    public ResponseEntity<ApiResponse<AttachmentResponse>> upload(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {
        AttachmentResponse response = attachmentService.upload(taskId, file, currentUser());
        return ResponseEntity.status(201).body(new ApiResponse<>(201, true, "Attachment uploaded.", response));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "Remove a file attachment")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long attachmentId) {
        attachmentService.delete(attachmentId, currentUser());
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Attachment deleted."));
    }

    private Users currentUser() {
        return (Users) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
