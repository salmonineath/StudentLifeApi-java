package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.Email.EmailService;
import com.studentlife.studentlifejava.dto.request.InviteRequest;
import com.studentlife.studentlifejava.dto.response.InviteResponse;
import com.studentlife.studentlifejava.dto.response.MemberResponse;
import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.AssignmentInvite;
import com.studentlife.studentlifejava.entity.InviteStatus;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.AssignmentInviteRepository;
import com.studentlife.studentlifejava.repository.AssignmentRepository;
import com.studentlife.studentlifejava.repository.UserRepository;
import com.studentlife.studentlifejava.security.AssignmentAccessGuard;
import com.studentlife.studentlifejava.service.AssignmentInviteService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.*;

// KNOWN GAP: this class can create and revoke PENDING invites, but nothing in
// the codebase ever transitions an invite to ACCEPTED or DECLINED - there is no
// accept-invite endpoint/service method that consumes AssignmentInvite.token.
// Practically, this means invited collaborators can never actually join an
// assignment: AssignmentAccessGuard#isAcceptedMember can never see an ACCEPTED
// row, so requireMember() only ever admits the assignment owner or a global
// admin. Needs a real "accept invite by token" flow (and ideally linking
// invitedUser for invites sent to an email that signs up later) before this
// feature is usable end-to-end.
@Service
@RequiredArgsConstructor
public class AssignmentInviteServiceImpl implements AssignmentInviteService {

    private static final String[] PALETTE = {
            "#10B981", "#F59E0B", "#3B82F6", "#EF4444", "#8B5CF6", "#EC4899", "#14B8A6", "#F97316"
    };

    private final AssignmentRepository assignmentRepository;
    private final AssignmentInviteRepository assignmentInviteRepository;
    private final UserRepository userRepository;
    private final AssignmentAccessGuard accessGuard;
    private final EmailService emailService;

    @Override
    @Transactional
    public InviteResponse invite(Long assignmentId, InviteRequest request, Users currentUser) {
        Assignment assignment = findAssignment(assignmentId);
        accessGuard.requireOwnerOrAdmin(assignment, currentUser);

        String email = request.getEmail().toLowerCase();

        assignmentInviteRepository.findByAssignmentAndEmailIgnoreCaseAndStatus(assignment, email, InviteStatus.PENDING)
                .ifPresent(existing -> {
                    throw badRequest("This email already has a pending invite for this assignment.");
                });

        Users invitedUser = userRepository.findByEmail(email).orElse(null);

        AssignmentInvite invite = AssignmentInvite.builder()
                .assignment(assignment)
                .email(email)
                .invitedUser(invitedUser)
                .status(InviteStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .invitedBy(currentUser)
                .build();
        AssignmentInvite saved = assignmentInviteRepository.save(invite);

        // Sent inside the same transaction as the save: if the SMTP call throws or
        // is slow, the invite row rolls back too and the DB connection is held
        // open for the round trip. Acceptable at current volume; move this after
        // commit (async/event listener) if invite email becomes unreliable or
        // this method starts showing up in slow-request logs.
        emailService.sendAssignmentInviteEmail(email, currentUser.getFullname(), assignment.getTitle());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void revoke(Long assignmentId, String email, Users currentUser) {
        Assignment assignment = findAssignment(assignmentId);
        accessGuard.requireOwnerOrAdmin(assignment, currentUser);

        AssignmentInvite invite = assignmentInviteRepository
                .findByAssignmentAndEmailIgnoreCaseAndStatus(assignment, email.toLowerCase(), InviteStatus.PENDING)
                .orElseThrow(() -> notFound("No pending invite found for this email."));
        assignmentInviteRepository.delete(invite);
    }

    @Override
    public List<MemberResponse> members(Long assignmentId, Users currentUser) {
        Assignment assignment = findAssignment(assignmentId);
        accessGuard.requireMember(assignment, currentUser);

        List<Users> members = new ArrayList<>();
        members.add(assignment.getCreatedBy());

        assignmentInviteRepository.findByAssignmentAndStatus(assignment, InviteStatus.ACCEPTED)
                .forEach(invite -> {
                    if (invite.getInvitedUser() != null) {
                        members.add(invite.getInvitedUser());
                    }
                });

        return members.stream().map(this::toMemberResponse).toList();
    }

    @Override
    @Transactional
    public void acceptInvite(String token, Users currentUser) {
        AssignmentInvite invite = assignmentInviteRepository
                .findByToken(token)
                .orElseThrow(() -> notFound("This invite link is invalid"));

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw badRequest("This invite has already been used");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setInvitedUser(currentUser);
        assignmentInviteRepository.save(invite);
    }

    private Assignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> notFound("Assignment not found."));
    }

    private InviteResponse toResponse(AssignmentInvite invite) {
        return InviteResponse.builder()
                .id(invite.getId())
                .assignmentId(invite.getAssignment().getId())
                .email(invite.getEmail())
                .status(invite.getStatus())
                .createdAt(invite.getCreatedAt())
                .build();
    }

    // Color is deterministic per user id (same user always renders with the same
    // avatar color across requests/sessions), not random.
    private MemberResponse toMemberResponse(Users user) {
        return MemberResponse.builder()
                .id(user.getId())
                .name(user.getFullname())
                .initials(deriveInitials(user))
                .color(PALETTE[(int) (Math.abs(user.getId()) % PALETTE.length)])
                .build();
    }

    private String deriveInitials(Users user) {
        String source = (user.getFullname() != null && !user.getFullname().isBlank())
                ? user.getFullname()
                : user.getUsername();
        String[] parts = source.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return initials.toString();
    }
}
