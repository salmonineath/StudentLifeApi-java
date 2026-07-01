ALTER TABLE roles
    ADD CONSTRAINT uq_roles_name UNIQUE (name);
