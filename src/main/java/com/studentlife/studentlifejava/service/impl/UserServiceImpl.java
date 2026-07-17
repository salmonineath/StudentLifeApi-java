package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.Email.EmailService;
import com.studentlife.studentlifejava.dto.request.UpdateUserByAdminRequest;
import com.studentlife.studentlifejava.dto.response.CurrentUserResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.mapper.UserMapper;
import com.studentlife.studentlifejava.repository.UserRepository;
import com.studentlife.studentlifejava.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.notFound;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Override
    public CurrentUserResponse getCurrentUser(Users users) {
        return userMapper.toCurrentUserResponse(users);
    }

    @Override
    public List<CurrentUserResponse> getAllUser() {
        return userRepository.findAll().stream()
                .map(userMapper::toCurrentUserResponse)
                .toList();
    }

    @Override
    public CurrentUserResponse getUserById(Long id) {
        Users users = userRepository.findById(id)
                .orElseThrow(() -> notFound("User not found"));
        return userMapper.toCurrentUserResponse(users);
    }

    @Override
    @Transactional
    public void banUser(Long id, UpdateUserByAdminRequest request) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> notFound("User not found"));
        user.setIsActive(false);
        user.setBanReason(request.getReason());
        userRepository.save(user);

        // Best-effort notification: the ban itself must not be reported as failed
        // (or rolled back) just because SMTP is down - the admin's intent already
        // took effect above.
        try {
            emailService.sendAccountBannedEmail(user.getEmail(), request.getReason());
        } catch (MailException e) {
            log.error("Ban applied for user id={} but notification email failed", id, e);
        }
    }

    @Override
    @Transactional
    public void unBanUser(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> notFound("User not found"));

        user.setIsActive(true);
        user.setBanReason(null);
        userRepository.save(user);
    }
}
