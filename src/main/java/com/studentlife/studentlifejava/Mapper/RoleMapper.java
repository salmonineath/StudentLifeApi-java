package com.studentlife.studentlifejava.Mapper;

import com.studentlife.studentlifejava.Entity.Role;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfiguration.class)
public interface RoleMapper {

    // Entity → Response
    default String map(Role role) {
        return role.getName();
    }

    // Request → Entity (used by MapStruct internally)
    default Role map(Long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }
}
