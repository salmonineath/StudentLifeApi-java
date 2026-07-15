package com.studentlife.studentlifejava.mapper;

import com.studentlife.studentlifejava.dto.request.RegisterRequest;
import com.studentlife.studentlifejava.dto.response.AuthUserResponse;
import com.studentlife.studentlifejava.dto.response.UserResponse;
import com.studentlife.studentlifejava.entity.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        config = MapperConfiguration.class,
        uses = {RoleMapper.class}
)
public interface UserMapper {

    AuthUserResponse toAuthUserResponse(Users user);

    UserResponse toUserResponse(Users users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "university", ignore = true)
    @Mapping(target = "major", ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Users toUserEntityRegisterUser(RegisterRequest request);
}