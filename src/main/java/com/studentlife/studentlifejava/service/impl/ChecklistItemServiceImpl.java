package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.request.ChecklistItemRequest;
import com.studentlife.studentlifejava.dto.request.ChecklistItemToggleRequest;
import com.studentlife.studentlifejava.dto.response.ChecklistItemResponse;
import com.studentlife.studentlifejava.entity.ChecklistItem;
import com.studentlife.studentlifejava.entity.Task;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.ChecklistItemRepository;
import com.studentlife.studentlifejava.repository.TaskRepository;
import com.studentlife.studentlifejava.security.AssignmentAccessGuard;
import com.studentlife.studentlifejava.service.ChecklistItemService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.notFound;

@Service
@RequiredArgsConstructor
public class ChecklistItemServiceImpl implements ChecklistItemService {

    private final ChecklistItemRepository checklistItemRepository;
    private final TaskRepository taskRepository;
    private final AssignmentAccessGuard accessGuard;

    @Override
    @Transactional
    public ChecklistItemResponse create(Long taskId, ChecklistItemRequest request, Users currentUser) {
        Task task = findTask(taskId);
        accessGuard.requireMember(task.getAssignment(), currentUser);

        ChecklistItem item = ChecklistItem.builder()
                .task(task)
                .text(request.getText())
                .done(false)
                .build();
        return toResponse(checklistItemRepository.save(item));
    }

    @Override
    @Transactional
    public ChecklistItemResponse toggle(Long itemId, ChecklistItemToggleRequest request, Users currentUser) {
        ChecklistItem item = findItem(itemId);
        accessGuard.requireMember(item.getTask().getAssignment(), currentUser);

        item.setDone(request.getDone());
        return toResponse(checklistItemRepository.save(item));
    }

    @Override
    @Transactional
    public void delete(Long itemId, Users currentUser) {
        ChecklistItem item = findItem(itemId);
        accessGuard.requireMember(item.getTask().getAssignment(), currentUser);
        checklistItemRepository.delete(item);
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> notFound("Task not found."));
    }

    private ChecklistItem findItem(Long itemId) {
        return checklistItemRepository.findById(itemId)
                .orElseThrow(() -> notFound("Checklist item not found."));
    }

    private ChecklistItemResponse toResponse(ChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .taskId(item.getTask().getId())
                .text(item.getText())
                .done(item.getDone())
                .build();
    }
}
