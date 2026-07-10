package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.response.AttachmentResponse;
import com.studentlife.studentlifejava.entity.Attachment;
import com.studentlife.studentlifejava.entity.Task;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.AttachmentRepository;
import com.studentlife.studentlifejava.repository.TaskRepository;
import com.studentlife.studentlifejava.security.AssignmentAccessGuard;
import com.studentlife.studentlifejava.service.AttachmentService;
import com.studentlife.studentlifejava.service.CloudinaryService;
import com.studentlife.studentlifejava.service.CloudinaryUploadResult;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.notFound;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final AssignmentAccessGuard accessGuard;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public AttachmentResponse upload(Long taskId, MultipartFile file, Users currentUser) {
        Task task = findTask(taskId);
        accessGuard.requireMember(task.getAssignment(), currentUser);

        CloudinaryUploadResult result = cloudinaryService.upload(file);

        Attachment attachment = Attachment.builder()
                .task(task)
                .name(file.getOriginalFilename())
                .sizeBytes(file.getSize())
                .url(result.url())
                .publicId(result.publicId())
                .resourceType(result.resourceType())
                .uploadedBy(currentUser)
                .uploadedAt(Instant.now())
                .build();
        return toResponse(attachmentRepository.save(attachment));
    }

    @Override
    @Transactional
    public void delete(Long attachmentId, Users currentUser) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> notFound("Attachment not found."));
        accessGuard.requireMember(attachment.getTask().getAssignment(), currentUser);

        cloudinaryService.delete(attachment.getPublicId(), attachment.getResourceType());
        attachmentRepository.delete(attachment);
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> notFound("Task not found."));
    }

    private AttachmentResponse toResponse(Attachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .taskId(a.getTask().getId())
                .name(a.getName())
                .sizeBytes(a.getSizeBytes())
                .url(a.getUrl())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
