package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.response.MemberResponse;
import com.studentlife.studentlifejava.entity.Users;

import java.util.List;

public interface UserSearchService {

    List<MemberResponse> searchByEmail(String query, Users currentUser);
}
