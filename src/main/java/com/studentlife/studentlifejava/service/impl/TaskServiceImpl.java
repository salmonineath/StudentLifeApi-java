package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.request.TaskReorderRequest;
import com.studentlife.studentlifejava.dto.request.TaskRequest;
import com.studentlife.studentlifejava.dto.request.TaskStatusUpdateRequest;
import com.studentlife.studentlifejava.dto.response.AttachmentResponse;
import com.studentlife.studentlifejava.dto.response.ChecklistItemResponse;
import com.studentlife.studentlifejava.dto.response.TaskResponse;
import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.Attachment;
import com.studentlife.studentlifejava.entity.ChecklistItem;
import com.studentlife.studentlifejava.entity.Task;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.AssignmentRepository;
import com.studentlife.studentlifejava.repository.AttachmentRepository;
import com.studentlife.studentlifejava.repository.ChecklistItemRepository;
import com.studentlife.studentlifejava.repository.TaskRepository;
import com.studentlife.studentlifejava.repository.UserRepository;
import com.studentlife.studentlifejava.security.AssignmentAccessGuard;
import com.studentlife.studentlifejava.service.AssignmentService;
import com.studentlife.studentlifejava.service.CloudinaryService;
import com.studentlife.studentlifejava.service.TaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.*;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final AttachmentRepository attachmentRepository;
    private final AssignmentAccessGuard accessGuard;
    private final AssignmentService assignmentService;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public TaskResponse create(Long assignmentId, TaskRequest request, Users currentUser) {
        Assignment assignment = findAssignment(assignmentId);
        accessGuard.requireMember(assignment, currentUser);

        // Must derive from the max sortOrder, not a row count - deleting a task
        // leaves a gap, and countByAssignment would reissue an already-taken order.
        long nextOrder = taskRepository.findMaxSortOrder(assignment) + 1;

        Task task = Task.builder()
                .assignment(assignment)
                .title(request.getTitle())
                .description(request.getDescription())
                .sortOrder((int) nextOrder)
                .assignees(resolveAssignees(request.getAssigneeIds()))
                .build();
        Task saved = taskRepository.save(task);
        assignmentService.recalculateProgress(assignmentId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse update(Long taskId, TaskRequest request, Users currentUser) {
        Task task = findTask(taskId);
        accessGuard.requireMember(task.getAssignment(), currentUser);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setAssignees(resolveAssignees(request.getAssigneeIds()));
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public void delete(Long taskId, Users currentUser) {
        Task task = findTask(taskId);
        accessGuard.requireMember(task.getAssignment(), currentUser);
        Long assignmentId = task.getAssignment().getId();

        // The DB cascade silently removes the attachment rows with the task, so
        // their Cloudinary files must be collected here - after the cascade there
        // is no record left to find them by, and they'd be orphaned (and billed)
        // forever.
        List<Attachment> attachments = attachmentRepository.findByTask(task);
        taskRepository.delete(task);
        assignmentService.recalculateProgress(assignmentId);
        cloudinaryService.deleteAllAfterCommit(attachments);
    }

    @Override
    @Transactional
    public TaskResponse updateStatus(Long taskId, TaskStatusUpdateRequest request, Users currentUser) {
        Task task = findTask(taskId);
        accessGuard.requireMember(task.getAssignment(), currentUser);
        task.setStatus(request.getStatus());
        Task saved = taskRepository.save(task);
        assignmentService.recalculateProgress(task.getAssignment().getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<TaskResponse> reorder(Long assignmentId, TaskReorderRequest request, Users currentUser) {
        Assignment assignment = findAssignment(assignmentId);
        accessGuard.requireMember(assignment, currentUser);

        List<Task> tasks = taskRepository.findByAssignmentOrderBySortOrderAsc(assignment);
        Set<Long> existingIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(request.getOrderedIds());

        // Set equality (not subset) rejects partial reorders outright - the client
        // must submit a full permutation of this assignment's task ids.
        if (!existingIds.equals(requestedIds)) {
            throw badRequest("orderedIds must contain exactly the tasks belonging to this assignment.");
        }

        // indexOf() in a loop is O(n^2), fine for the small per-assignment task
        // lists this feature targets. Swap to an id->index map first if that changes.
        List<Long> orderedIds = request.getOrderedIds();
        for (Task task : tasks) {
            task.setSortOrder(orderedIds.indexOf(task.getId()));
        }
        return toResponses(taskRepository.saveAll(tasks));
    }

    private Assignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> notFound("Assignment not found."));
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> notFound("Task not found."));
    }

    private Set<Users> resolveAssignees(List<Long> assigneeIds) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Users> resolved = userRepository.findAllById(assigneeIds);
        if (resolved.size() != new HashSet<>(assigneeIds).size()) {
            throw badRequest("One or more assigneeIds do not correspond to an existing user.");
        }
        return new HashSet<>(resolved);
    }

    private TaskResponse toResponse(Task t) {
        return TaskResponse.builder()
                .id(t.getId())
                .assignmentId(t.getAssignment().getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .assigneeIds(t.getAssignees().stream().map(Users::getId).toList())
                .checklist(checklistItemRepository.findByTaskOrderByIdAsc(t).stream()
                        .map(this::toChecklistItemResponse).toList())
                .attachments(attachmentRepository.findByTask(t).stream()
                        .map(this::toAttachmentResponse).toList())
                .sortOrder(t.getSortOrder())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private List<TaskResponse> toResponses(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ChecklistItemResponse>> checklistByTaskId = checklistItemRepository
                .findByTaskInOrderByIdAsc(tasks).stream()
                .map(this::toChecklistItemResponse)
                .collect(Collectors.groupingBy(ChecklistItemResponse::getTaskId));
        Map<Long, List<AttachmentResponse>> attachmentsByTaskId = attachmentRepository
                .findByTaskIn(tasks).stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.groupingBy(AttachmentResponse::getTaskId));

        return tasks.stream().map(t -> TaskResponse.builder()
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
                .build()).toList();
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
