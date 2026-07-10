package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.request.ChecklistItemRequest;
import com.studentlife.studentlifejava.dto.request.ChecklistItemToggleRequest;
import com.studentlife.studentlifejava.dto.response.ChecklistItemResponse;
import com.studentlife.studentlifejava.entity.Users;

public interface ChecklistItemService {

    ChecklistItemResponse create(Long taskId, ChecklistItemRequest request, Users currentUser);

    ChecklistItemResponse toggle(Long itemId, ChecklistItemToggleRequest request, Users currentUser);

    void delete(Long itemId, Users currentUser);
}
