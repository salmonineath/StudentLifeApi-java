
CREATE TABLE roles (
                       id         BIGSERIAL PRIMARY KEY,
                       name       VARCHAR(255),
                       created_at TIMESTAMPTZ NOT NULL,
                       updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       fullname      VARCHAR(255),
                       username      VARCHAR(255) NOT NULL UNIQUE,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password      VARCHAR(255) NOT NULL,
                       university    VARCHAR(255),
                       major         VARCHAR(255),
                       academic_year VARCHAR(255),
                       is_active     BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ NOT NULL,
                       updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_role (
                           user_id BIGINT NOT NULL REFERENCES users(id),
                           role_id BIGINT NOT NULL REFERENCES roles(id),
                           PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_token (
                               id          BIGSERIAL PRIMARY KEY,
                               token_hash  VARCHAR(255) NOT NULL UNIQUE,
                               user_id     BIGINT NOT NULL REFERENCES users(id),
                               expired_at  TIMESTAMPTZ NOT NULL,
                               revoked     BOOLEAN NOT NULL,
                               rotated_at  TIMESTAMPTZ,
                               created_at  TIMESTAMPTZ NOT NULL
);