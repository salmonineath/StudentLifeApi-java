package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.response.MemberResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.AssignmentInviteRepository;
import com.studentlife.studentlifejava.repository.UserRepository;
import com.studentlife.studentlifejava.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSearchServiceImpl implements UserSearchService {

    private static final int MAX_RESULTS = 10;

    private static final String[] PALETTE = {
            "#10B981", "#F59E0B", "#3B82F6", "#EF4444", "#8B5CF6", "#EC4899", "#14B8A6", "#F97316"
    };

    private final UserRepository userRepository;
    private final AssignmentInviteRepository assignmentInviteRepository;

    @Override
    public List<MemberResponse> searchByEmail(String query, Users currentUser) {
        Optional<Users> exact = userRepository.findByEmail(query);
        if (exact.isPresent()) {
            return List.of(toResponse(exact.get()));
        }

        List<String> knownEmails = assignmentInviteRepository
                .findInvitedEmailsByOwnerAndPrefix(currentUser, query);

        if (knownEmails.isEmpty()) {
            return List.of();
        }

        return userRepository.findByEmailInIgnoreCase(knownEmails, PageRequest.of(0, MAX_RESULTS))
                .stream().map(this::toResponse).toList();
    }

    private MemberResponse toResponse(Users user) {
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
