package com.studentlife.studentlifejava.DTO.Request;

import com.studentlife.studentlifejava.Entity.Role;
import lombok.Data;

import java.util.Set;

@Data
public class UserRequest {

    private String username;
    private String email;
    private String password;

    private String fullname;

    private String university;
    private String major;
    private String academicYear;

    private Set<Role> roles;
}