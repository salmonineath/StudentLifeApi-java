package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.request.AssignmentRequest;
import com.studentlife.studentlifejava.dto.response.AssignmentResponse;
import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.AssignmentRepository;
import com.studentlife.studentlifejava.service.AssignmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.*;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;

    @Override
    @Transactional
    public AssignmentResponse create(AssignmentRequest request, Users currentUser) {
        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
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
    public AssignmentResponse get(Long id, Users currentUser) {
        return toResponse(findAndAuthorize(id, currentUser));
    }

    @Override
    @Transactional
    public AssignmentResponse update(Long id, AssignmentRequest request, Users currentUser) {
        Assignment assignment = findAndAuthorize(id, currentUser);
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setCourseId(request.getCourseId());
        return toResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void delete(Long id, Users currentUser) {
        assignmentRepository.delete(findAndAuthorize(id, currentUser));
    }

    private Assignment findAndAuthorize(Long id, Users currentUser) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> notFound("Assignment not found."));

        boolean isOwner = assignment.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin"));

        if (!isOwner && !isAdmin) {
            throw forbidden("You are not allowed to access this assignment.");
        }
        return assignment;
    }

    private AssignmentResponse toResponse(Assignment a) {
        return AssignmentResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .courseId(a.getCourseId())
                .createdById(a.getCreatedBy().getId())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
