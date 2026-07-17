package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.request.InviteRequest;
import com.studentlife.studentlifejava.dto.response.InviteResponse;
import com.studentlife.studentlifejava.dto.response.MemberResponse;
import com.studentlife.studentlifejava.entity.Users;

import java.util.List;

public interface AssignmentInviteService {

    InviteResponse invite(Long assignmentId, InviteRequest request, Users currentUser);

    void revoke(Long assignmentId, String email, Users currentUser);

    List<MemberResponse> members(Long assignmentId, Users currentUser);

    void acceptInvite(String token, Users currentUser);
}
