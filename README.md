# StudentLife — Backend API

A digital productivity and collaboration platform for Cambodian university students. Students currently rely on scattered tools (Telegram, Facebook groups, handwritten notes) to manage academic life. StudentLife centralizes everything into one platform.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Build Tool | Maven |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL (Neon serverless) |
| Real-time | Spring WebSocket — STOMP over SockJS |
| Email | Spring Mail — Gmail SMTP |
| Image Storage | Cloudinary |
| AI Integration | Groq API — Llama 3.1 8B Instant |
| Push Notifications | OneSignal |
| Code Generation | Lombok, MapStruct 1.5.5 |
| API Docs | SpringDoc OpenAPI 2.8.8 (Swagger UI) |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions + Render |

---

## Getting Started Locally

### Prerequisites

- Java 21
- Maven
- Docker & Docker Compose (or a local PostgreSQL instance)

### Option 1: Run with Docker

```bash
docker compose up --build
```

### Option 2: Run with Maven

```bash
./mvnw spring-boot:run
```

Server starts on **port 5000** by default.

### Environment Variables

Create a `.env` file in the project root with the following:

```env
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
JWT_SECRET=
JWT_EXPIRATION=
JWT_REFRESH_EXPIRATION=
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
MAIL_USERNAME=
MAIL_PASSWORD=
GROQ_API_KEY=
ONESIGNAL_APP_ID=
ONESIGNAL_API_KEY=
FRONTEND_URL=
BACKEND_URL=
ADMIN_EMAIL=
ADMIN_PASSWORD=
ADMIN_USERNAME=
```

---

## Project Structure

```
src/main/java/com/studentlife/studentlifejava/
├── Config/          # Spring beans (security, app config)
├── Controller/      # REST controllers
├── DTO/
│   ├── Request/     # Incoming request bodies
│   └── Response/    # Outgoing response shapes
├── Entity/          # JPA entities (database tables)
├── Exception/       # Custom exceptions + global handler
├── JWT/             # JWT generation and validation
├── Repository/      # Spring Data JPA repositories
├── Security/        # Security filter chain config
└── Service/
    └── Impl/        # Service implementations
```

---

## Database Schema

12 entities total:

| Entity | Purpose |
|---|---|
| `Users` | Auth info, university details, roles, active status |
| `Roles` | Role definitions — `ROLE_ADMIN`, `ROLE_STUDENT` |
| `UserProfile` | Extended profile data (one-to-one with Users) |
| `Assignments` | Projects with title, subject, due date, status, progress |
| `AssignmentMembers` | Collaboration — invite tokens, status (`INVITED` / `ACCEPTED` / `DECLINED`) |
| `Schedules` | One-time or recurring calendar events |
| `GroupMessage` | Chat messages scoped to an assignment |
| `GroupChatMember` | Join/leave timestamps per user per group |
| `Notification` | In-app notifications with type and read status |
| `UserDevices` | Device info per user session |
| `RefreshToken` | Stored refresh tokens with expiry for rotation |
| `ReminderLog` | Tracks last reminder sent per assignment |
| `PasswordOTP` | OTP records for password reset flow |

---

## API Reference

**Base URL:** `https://studentlifeapis.onrender.com`  
**Version prefix:** `/api/v1`  
**Swagger UI:** `/swagger-ui.html`  
**OpenAPI JSON:** `/v3/docs`

| Module | Prefix | Key Endpoints |
|---|---|---|
| Authentication | `/api/v1/auth` | register, login, refresh, logout |
| Password Reset | `/api/v1/auth` | otp-request, verify-otp, reset-password |
| Current User | `/api/v1/me` | get profile, update profile, devices |
| Admin — Users | `/api/v1/admin/users` | list, get, create, update, disable, enable, delete |
| Assignments | `/api/v1/assignments` | CRUD, progress, invite, join, members |
| Schedules | `/api/v1/schedule` | one-time, recurring, update, delete |
| Group Chat (REST) | `/api/v1/chat` | history, members, clear |
| Notifications | `/api/v1/notification` | list, unread count, mark read, delete |
| AI Study Plan | `/api/v1/study-plan` | generate per assignment |
| Push Notifications | `/api/v1/register` | register OneSignal player ID |
| Health Check | `/health` | server status |

---

## Real-time (WebSocket)

WebSocket endpoint: `/api/v1/ws` (STOMP over SockJS)  
Authentication: JWT passed on STOMP handshake.

| Channel | Purpose |
|---|---|
| `/app/chat.send` | Send a message to an assignment group chat |
| `/app/chat.join` | Broadcast join presence event |
| `/app/chat.leave` | Broadcast leave presence event |
| `/topic/group/{assignmentId}` | Subscribe to incoming messages |
| `/topic/presence/{assignmentId}` | Subscribe to presence events |
| `/user/queue/notifications` | Subscribe to personal notifications |

---

## Security Model

- Passwords hashed with **BCrypt**
- JWT stored in **HTTP-only cookies** (HTTPS-only in production) and optionally as Bearer tokens
- Access token: **15-minute** expiry
- Refresh token: **30-day** expiry with rotation
- Role-based authorization via `@PreAuthorize`
- WebSocket connections authenticated via `WebSocketAuthInterceptor`
- Errors never expose stack traces to clients

---

## Core Features

1. **Authentication** — Register, login, JWT refresh, logout, role-based access
2. **Password Reset** — OTP sent to email, verify, then reset
3. **User Profile** — View/update profile, upload avatar via Cloudinary
4. **Dashboard** — Today's schedule, assignment summary, upcoming deadlines
5. **Schedule Management** — One-time and recurring events
6. **Assignment Tracker** — Create, track progress (0–100%), statuses: `PENDING` / `IN_PROGRESS` / `COMPLETED` / `OVERDUE`
7. **Collaboration** — Invite by email or shareable token link
8. **Group Chat** — Real-time per-assignment chat, paginated history, auto-delete after 5 days
9. **Notifications** — In-app, email (deadline reminders at 72h / 24h / 2h), push via OneSignal
10. **Device Tracking** — Tracks browser, OS, IP per session
11. **AI Study Plan** — Generates a day-by-day study plan using Groq + Llama 3.1 8B

---

## CI/CD Pipeline

**CI** triggers on push/PR to `develop` or `main`:
1. Checkout → Java 21 setup → Maven cache → Build → Test

**CD** triggers on push to `main` only:
1. Sends deploy hook to Render → Render rebuilds Docker container

### Infrastructure

| Component | Platform |
|---|---|
| Backend | Render.com (Docker, port 5000) |
| Database | Neon serverless PostgreSQL |
| Image Storage | Cloudinary |

---

## Roles

| Role | Access |
|---|---|
| `ROLE_STUDENT` | Default on register — access to own data, assignments, chat |
| `ROLE_ADMIN` | Full access — user management, admin endpoints |
