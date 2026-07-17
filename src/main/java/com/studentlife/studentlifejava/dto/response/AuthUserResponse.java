package com.studentlife.studentlifejava.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private Long id;
    private String fullname;
    private String username;
    private String email;
    private String university;
    private String major;
    private String academicYear;
    private Set<String> roles;
}
