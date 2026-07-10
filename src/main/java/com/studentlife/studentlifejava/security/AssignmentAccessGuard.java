package com.studentlife.studentlifejava.security;

import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.InviteStatus;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.AssignmentInviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.forbidden;

@Component
@RequiredArgsConstructor
public class AssignmentAccessGuard {

    private final AssignmentInviteRepository assignmentInviteRepository;

    public void requireOwnerOrAdmin(Assignment assignment, Users currentUser) {
        if (!isOwner(assignment, currentUser) && !isAdmin(currentUser)) {
            throw forbidden("You are not allowed to perform this action.");
        }
    }

    public void requireMember(Assignment assignment, Users currentUser) {
        if (!isOwner(assignment, currentUser) && !isAdmin(currentUser) && !isAcceptedMember(assignment, currentUser)) {
            throw forbidden("You are not a member of this assignment.");
        }
    }

    private boolean isOwner(Assignment assignment, Users currentUser) {
        return assignment.getCreatedBy().getId().equals(currentUser.getId());
    }

    private boolean isAdmin(Users currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin"));
    }

    private boolean isAcceptedMember(Assignment assignment, Users currentUser) {
        return assignmentInviteRepository.findByAssignmentAndStatus(assignment, InviteStatus.ACCEPTED)
                .stream()
                .anyMatch(invite -> invite.getInvitedUser() != null
                        && invite.getInvitedUser().getId().equals(currentUser.getId()));
    }
}
