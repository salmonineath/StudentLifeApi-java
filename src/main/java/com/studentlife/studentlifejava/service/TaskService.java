package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.request.TaskReorderRequest;
import com.studentlife.studentlifejava.dto.request.TaskRequest;
import com.studentlife.studentlifejava.dto.request.TaskStatusUpdateRequest;
import com.studentlife.studentlifejava.dto.response.TaskResponse;
import com.studentlife.studentlifejava.entity.Users;

import java.util.List;

public interface TaskService {

    TaskResponse create(Long assignmentId, TaskRequest request, Users currentUser);

    TaskResponse update(Long taskId, TaskRequest request, Users currentUser);

    void delete(Long taskId, Users currentUser);

    TaskResponse updateStatus(Long taskId, TaskStatusUpdateRequest request, Users currentUser);

    List<TaskResponse> reorder(Long assignmentId, TaskReorderRequest request, Users currentUser);
}
