package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.request.AssignmentRequest;
import com.studentlife.studentlifejava.dto.response.AssignmentResponse;
import com.studentlife.studentlifejava.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssignmentService {

    AssignmentResponse create(AssignmentRequest request, Users currentUser);

    Page<AssignmentResponse> list(Users currentUser, Pageable pageable);

    AssignmentResponse get(Long id, Users currentUser);

    AssignmentResponse update(Long id, AssignmentRequest request, Users currentUser);

    void delete(Long id, Users currentUser);
}
