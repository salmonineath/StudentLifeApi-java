ALTER TABLE assignments
    ADD COLUMN subject    VARCHAR(255),
    ADD COLUMN progress   INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN completed  BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE tasks (
    id            BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'todo',
    sort_order    INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tasks_assignment_id ON tasks(assignment_id);
CREATE INDEX idx_tasks_assignment_id_sort_order ON tasks(assignment_id, sort_order);

CREATE TABLE task_assignees (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE checklist_items (
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    text       VARCHAR(1000) NOT NULL,
    done       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_checklist_items_task_id ON checklist_items(task_id);

CREATE TABLE attachments (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    size_bytes  BIGINT NOT NULL,
    url         VARCHAR(1000) NOT NULL,
    public_id   VARCHAR(255) NOT NULL,
    uploaded_by BIGINT REFERENCES users(id),
    uploaded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_attachments_task_id ON attachments(task_id);

CREATE TABLE assignment_invites (
    id              BIGSERIAL PRIMARY KEY,
    assignment_id   BIGINT NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    invited_user_id BIGINT REFERENCES users(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    token           VARCHAR(64) NOT NULL UNIQUE,
    invited_by      BIGINT NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_assignment_invites_assignment_id ON assignment_invites(assignment_id);
CREATE INDEX idx_assignment_invites_email ON assignment_invites(email);
CREATE UNIQUE INDEX idx_assignment_invites_pending_unique
    ON assignment_invites(assignment_id, email)
    WHERE status = 'PENDING';

CREATE INDEX idx_users_email_lower ON users(lower(email));
