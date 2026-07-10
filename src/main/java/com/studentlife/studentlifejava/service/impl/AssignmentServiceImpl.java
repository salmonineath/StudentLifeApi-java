package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.request.AssignmentRequest;
import com.studentlife.studentlifejava.dto.response.AssignmentDetailResponse;
import com.studentlife.studentlifejava.dto.response.AssignmentResponse;
import com.studentlife.studentlifejava.dto.response.AttachmentResponse;
import com.studentlife.studentlifejava.dto.response.ChecklistItemResponse;
import com.studentlife.studentlifejava.dto.response.TaskResponse;
import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.AssignmentInvite;
import com.studentlife.studentlifejava.entity.Attachment;
import com.studentlife.studentlifejava.entity.ChecklistItem;
import com.studentlife.studentlifejava.entity.InviteStatus;
import com.studentlife.studentlifejava.entity.Task;
import com.studentlife.studentlifejava.entity.TaskStatus;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.AssignmentInviteRepository;
import com.studentlife.studentlifejava.repository.AssignmentRepository;
import com.studentlife.studentlifejava.repository.AttachmentRepository;
import com.studentlife.studentlifejava.repository.ChecklistItemRepository;
import com.studentlife.studentlifejava.repository.TaskRepository;
import com.studentlife.studentlifejava.security.AssignmentAccessGuard;
import com.studentlife.studentlifejava.service.AssignmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.*;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final AttachmentRepository attachmentRepository;
    private final AssignmentInviteRepository assignmentInviteRepository;
    private final AssignmentAccessGuard accessGuard;

    @Override
    @Transactional
    public AssignmentResponse create(AssignmentRequest request, Users currentUser) {
        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .subject(request.getSubject())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .courseId(request.getCourseId())
                .createdBy(currentUser)
                .build();
        return toResponse(assignmentRepository.save(assignment));
    }

    @Override
    public Page<AssignmentResponse> list(Users currentUser, Pageable pageable) {
        return assignmentRepository.findByCreatedBy(currentUser, pageable).map(this::toResponse);
    }

    @Override
    public AssignmentDetailResponse get(Long id, Users currentUser) {
        Assignment assignment = find(id);
        accessGuard.requireMember(assignment, currentUser);
        List<Task> tasks = taskRepository.findByAssignmentOrderBySortOrderAsc(assignment);
        return toDetailResponse(assignment, tasks);
    }

    @Override
    @Transactional
    public AssignmentResponse update(Long id, AssignmentRequest request, Users currentUser) {
        Assignment assignment = find(id);
        accessGuard.requireOwnerOrAdmin(assignment, currentUser);
        assignment.setTitle(request.getTitle());
        assignment.setSubject(request.getSubject());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setCourseId(request.getCourseId());
        return toResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void delete(Long id, Users currentUser) {
        Assignment assignment = find(id);
        accessGuard.requireOwnerOrAdmin(assignment, currentUser);
        assignmentRepository.delete(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse toggleComplete(Long id, Users currentUser) {
        Assignment assignment = find(id);
        accessGuard.requireMember(assignment, currentUser);
        assignment.setCompleted(!assignment.getCompleted());
        return toResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void recalculateProgress(Long id) {
        Assignment assignment = find(id);
        List<Task> tasks = taskRepository.findByAssignmentOrderBySortOrderAsc(assignment);
        int progress = tasks.isEmpty()
                ? 0
                : (int) Math.round(100.0 * tasks.stream().filter(t -> t.getStatus() == TaskStatus.done).count() / tasks.size());
        assignment.setProgress(progress);
        assignmentRepository.save(assignment);
    }

    private Assignment find(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> notFound("Assignment not found."));
    }

    private AssignmentResponse toResponse(Assignment a) {
        return AssignmentResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .subject(a.getSubject())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .courseId(a.getCourseId())
                .progress(a.getProgress())
                .completed(a.getCompleted())
                .createdById(a.getCreatedBy().getId())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private AssignmentDetailResponse toDetailResponse(Assignment a, List<Task> tasks) {
        Map<Long, List<ChecklistItemResponse>> checklistByTaskId = tasks.isEmpty()
                ? Map.of()
                : checklistItemRepository.findByTaskInOrderByIdAsc(tasks).stream()
                        .map(this::toChecklistItemResponse)
                        .collect(Collectors.groupingBy(ChecklistItemResponse::getTaskId));
        Map<Long, List<AttachmentResponse>> attachmentsByTaskId = tasks.isEmpty()
                ? Map.of()
                : attachmentRepository.findByTaskIn(tasks).stream()
                        .map(this::toAttachmentResponse)
                        .collect(Collectors.groupingBy(AttachmentResponse::getTaskId));

        return AssignmentDetailResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .subject(a.getSubject())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .courseId(a.getCourseId())
                .progress(a.getProgress())
                .completed(a.getCompleted())
                .createdById(a.getCreatedBy().getId())
                .tasks(tasks.stream()
                        .map(t -> toTaskResponse(t, checklistByTaskId, attachmentsByTaskId))
                        .toList())
                .invites(assignmentInviteRepository.findByAssignmentAndStatus(a, InviteStatus.PENDING).stream()
                        .map(AssignmentInvite::getEmail).toList())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private TaskResponse toTaskResponse(Task t, Map<Long, List<ChecklistItemResponse>> checklistByTaskId,
                                         Map<Long, List<AttachmentResponse>> attachmentsByTaskId) {
        return TaskResponse.builder()
                .id(t.getId())
                .assignmentId(t.getAssignment().getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .assigneeIds(t.getAssignees().stream().map(Users::getId).toList())
                .checklist(checklistByTaskId.getOrDefault(t.getId(), List.of()))
                .attachments(attachmentsByTaskId.getOrDefault(t.getId(), List.of()))
                .sortOrder(t.getSortOrder())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private ChecklistItemResponse toChecklistItemResponse(ChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .taskId(item.getTask().getId())
                .text(item.getText())
                .done(item.getDone())
                .build();
    }

    private AttachmentResponse toAttachmentResponse(Attachment a) {
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
