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
        // "ROLE_admin" must match Users.getAuthorities()'s "ROLE_" + role.getName()
        // construction exactly - there's no compile-time link between this string
        // and the role name stored in the database, so a role rename/re-casing
        // silently breaks every admin check instead of failing loudly.
        return currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin"));
    }

    // NOTE: there is currently no flow anywhere that ever sets an invite's status
    // to ACCEPTED (invite/revoke only ever create or delete PENDING invites), so
    // this can never return true today - requireMember() is effectively equivalent
    // to requireOwnerOrAdmin() until an accept-invite endpoint exists.
    private boolean isAcceptedMember(Assignment assignment, Users currentUser) {
        // Loads every ACCEPTED invite for the assignment and scans in memory rather
        // than an indexed existsBy... query. Fine at today's scale; revisit once
        // the accept flow ships and assignments can have many real collaborators.
        return assignmentInviteRepository.findByAssignmentAndStatus(assignment, InviteStatus.ACCEPTED)
                .stream()
                .anyMatch(invite -> invite.getInvitedUser() != null
                        && invite.getInvitedUser().getId().equals(currentUser.getId()));
    }
}
