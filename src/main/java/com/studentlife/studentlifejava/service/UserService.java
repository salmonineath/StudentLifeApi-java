package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.request.UpdateUserByAdminRequest;
import com.studentlife.studentlifejava.dto.response.CurrentUserResponse;
import com.studentlife.studentlifejava.entity.Users;

import java.util.List;

public interface UserService {

    CurrentUserResponse getCurrentUser(Users users);

    List<CurrentUserResponse> getAllUser();

    CurrentUserResponse getUserById(Long id);

    void deactivateUser(Long id, UpdateUserByAdminRequest request);

    void activateUser(Long id);
}
