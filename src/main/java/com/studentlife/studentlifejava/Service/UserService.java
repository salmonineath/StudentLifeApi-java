package com.studentlife.studentlifejava.Service;

import org.springframework.data.domain.Page;
import com.studentlife.studentlifejava.DTO.Request.UserRequest;
import com.studentlife.studentlifejava.DTO.Response.UserResponse;

public interface UserService {

    Page<UserResponse> getAllUsers(int page, int size, String sortBy, String sortDir);
     UserResponse getUserById(Long id);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest request);

    void disableUser(Long id);

    void enableUser(Long id);

    void deleteUser(Long id);
    
}
