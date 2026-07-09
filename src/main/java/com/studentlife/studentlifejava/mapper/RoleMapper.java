package com.studentlife.studentlifejava.mapper;

import com.studentlife.studentlifejava.entity.Roles;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfiguration.class)
public interface RoleMapper {

    // Entity → Response
    default String map(Roles role) {
        return role.getName();
    }

    // Request → Entity (used by MapStruct internally)
    default Roles map(Long id) {
        Roles role = new Roles();
        role.setId(id);
        return role;
    }
}
