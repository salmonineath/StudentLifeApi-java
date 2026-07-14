# StudentLife — Backend API

A digital productivity and collaboration platform for university students. Students
currently juggle assignments, deadlines, and group work across scattered tools
(Telegram, Facebook groups, handwritten notes). StudentLife centralizes assignment
tracking, task management, and file sharing into a single backend API.

This is a Spring Boot REST API — the backend half of a final project, actively under
development.

---

## Core Features

- **Authentication** — register/login with JWT access + refresh tokens delivered as
  httpOnly cookies, refresh-token rotation with reuse detection, logout, and
  role-based access control (student / admin).
- **Password reset via OTP** — a 6-digit code emailed to the user, verified against a
  Redis-backed store, exchanged for a short-lived reset token to set a new password.
- **Assignment tracking** — create assignments with a subject, due date, and
  description; completion progress is calculated automatically as a percentage of
  completed tasks.
- **Task management** — per-assignment tasks with status (`todo` / `progress` /
  `done`), multiple assignees, manual reordering, checklists, and file attachments.
- **File attachments** — upload/delete task attachments backed by Cloudinary
  (images, documents, etc.).
- **Collaboration invites** — assignment owners can invite a collaborator by email
  (see [Under Development](#under-development--known-gaps) below for current
  limitations).
- **User search** — email-prefix autocomplete for adding collaborators.
- **Rate limiting** — per-IP request throttling on sensitive endpoints (login,
  register, OTP, search, invites) to slow down brute-force/abuse attempts.
- **Scheduled cleanup** — a daily job purges expired and revoked refresh tokens.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.15 |
| Build Tool | Maven |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Cache / OTP store | Redis |
| Rate limiting | Bucket4j + Caffeine |
| Email | Spring Mail (SMTP) |
| File storage | Cloudinary |
| Code generation | Lombok, MapStruct 1.5.5 |
| API docs | SpringDoc OpenAPI 2.7.0 (Swagger UI) |
| Containerization | Docker, Docker Compose, Nginx reverse proxy |

---

## How to Run

### Prerequisites

- Java 21
- Maven (or use the bundled `./mvnw` wrapper)
- Docker & Docker Compose, or a local PostgreSQL + Redis instance

### Environment Variables

Create a `.env` file in the project root:

```env
DATASOURCE_URL=
DATASOURCE_USERNAME=
DATASOURCE_PASSWORD=

JWT_SECRET=
JWT_ACCESS_TOKEN_EXPIRE=
JWT_REFRESH_TOKEN_EXPIRE=

REDIS_HOST=

MAIL_USERNAME=
MAIL_PASSWORD=

CLOUD_NAME=
API_KEY=
API_SECRET=

ADMIN_USERNAME=
ADMIN_EMAIL=
ADMIN_PASSWORD=

CORS_ALLOWED_ORIGINS=
```

### Option 1: Run with Docker

```bash
docker compose up --build
```

This starts the app, a Redis container, and an Nginx reverse proxy together.

### Option 2: Run with Maven

```bash
./mvnw spring-boot:run
```

The server starts on **port 5000** by default. Flyway runs migrations
automatically on startup; roles and an admin user are seeded automatically if
they don't already exist.

---

## Under Development / Known Gaps

- **Collaboration invites are not end-to-end functional yet.** An owner can send and
  revoke a pending invite, but there is no accept/decline flow yet — invited users
  can't actually become collaborators on an assignment until that's built.
- **Invite expiry** isn't implemented — pending invites don't currently expire.
- **Schedule management, group chat, in-app/push notifications, and AI-assisted
  study planning** are on the roadmap but not implemented in this codebase yet.

---

## License

Distributed under the MIT License. See [LICENSE](./LICENSE) for details.
