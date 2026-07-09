package com.studentlife.studentlifejava.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullname;
    private String username;
    private String email;
    private String university;
    private String major;
    private String academicYear;
    private Boolean isActive;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
