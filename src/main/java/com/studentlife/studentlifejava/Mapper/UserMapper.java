package com.studentlife.studentlifejava.Mapper;

import com.studentlife.studentlifejava.DTO.Request.RegisterRequest;
import com.studentlife.studentlifejava.DTO.Response.UserResponse;
import com.studentlife.studentlifejava.Entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        config = MapperConfiguration.class,
        uses = {RoleMapper.class }
)
public interface UserMapper {

    @Mapping(target = "roles", source = "roles")
    UserResponse toUserResponse(User user);

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "university",   ignore = true)
    @Mapping(target = "major",        ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    @Mapping(target = "roles",        ignore = true)
    @Mapping(target = "isActive",     ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    User toUserEntityRegisterUser(RegisterRequest request);
}
