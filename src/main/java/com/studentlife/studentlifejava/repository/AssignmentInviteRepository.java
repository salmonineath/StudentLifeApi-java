package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.AssignmentInvite;
import com.studentlife.studentlifejava.entity.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentInviteRepository extends JpaRepository<AssignmentInvite, Long> {
    List<AssignmentInvite> findByAssignment(Assignment assignment);

    List<AssignmentInvite> findByAssignmentAndStatus(Assignment assignment, InviteStatus status);

    Optional<AssignmentInvite> findByToken(String token);

    Optional<AssignmentInvite> findByAssignmentAndEmailIgnoreCaseAndStatus(
            Assignment assignment, String email, InviteStatus status);
}
