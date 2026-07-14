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
    // Builds an ID-only reference, not a fully-hydrated entity - name/timestamps
    // are left null. That's fine for its only purpose (letting JPA resolve the
    // relationship by id at save time); don't reuse this result for anything that
    // reads those other fields.
    default Roles map(Long id) {
        Roles role = new Roles();
        role.setId(id);
        return role;
    }
}
