package com.studentlife.studentlifejava.Service.Impl;

import com.studentlife.studentlifejava.DTO.Request.UserRequest;
import com.studentlife.studentlifejava.DTO.Response.UserResponse;
import com.studentlife.studentlifejava.Entity.User;
import com.studentlife.studentlifejava.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;

    // =========================
    // GET ALL USERS
    // =========================
    public Page<UserResponse> getAllUsers(int page, int size, String role, Boolean isActive) {

        Pageable pageable = PageRequest.of(page, size);

        Page<User> users = userRepository.findAll(pageable);

        return users.map(this::mapToResponse);
    }

    // =========================
    // GET USER BY ID
    // =========================
    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToResponse(user);
    }

    // =========================
    // CREATE USER
    // =========================
    public UserResponse createUser(UserRequest request) {

        User user = new User();

        user.setFullname(request.getFullname());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        user.setUniversity(request.getUniversity());
        user.setMajor(request.getMajor());
        user.setAcademicYear(request.getAcademicYear());

        user.setRoles(request.getRoles());

        user.setIsActive(true);
        user.setIsDeleted(false);

        return mapToResponse(userRepository.save(user));
    }

    // =========================
    // UPDATE USER
    // =========================
    public UserResponse updateUser(Long id, UserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullname(request.getFullname());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setUniversity(request.getUniversity());
        user.setMajor(request.getMajor());
        user.setAcademicYear(request.getAcademicYear());

        user.setRoles(request.getRoles());

        return mapToResponse(userRepository.save(user));
    }

    // =========================
    // DISABLE USER
    // =========================
    public void disableUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(false);

        userRepository.save(user);
    }

    // =========================
    // ENABLE USER
    // =========================
    public void enableUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(true);

        userRepository.save(user);
    }

    // =========================
    // DELETE USER (SOFT DELETE)
    // =========================
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsDeleted(true);

        userRepository.save(user);
    }

    // =========================
    // MAP ENTITY -> RESPONSE
    // =========================
    private UserResponse mapToResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .fullname(user.getFullname())
                .username(user.getUsername())
                .email(user.getEmail())
                .university(user.getUniversity())
                .major(user.getMajor())
                .academicYear(user.getAcademicYear())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}