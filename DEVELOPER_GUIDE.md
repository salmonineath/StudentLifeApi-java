# StudentLife Java — Developer Guide

Handover document for developers joining this project. Read this before touching any code.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Prerequisites](#3-prerequisites)
4. [Environment Setup](#4-environment-setup)
5. [Running the App](#5-running-the-app)
6. [Architecture](#6-architecture)
7. [Project Structure](#7-project-structure)
8. [Database Schema](#8-database-schema)
9. [API Endpoints](#9-api-endpoints)
10. [Authentication Flow](#10-authentication-flow)
11. [How to Add a New Feature](#11-how-to-add-a-new-feature)
12. [Key Rules](#12-key-rules)

---

## 1. Project Overview

StudentLife is a backend REST API built with Spring Boot. It handles student management features — starting with authentication and user management, with planned expansion into assignments, schedules, group messaging, and notifications.

Current state: authentication system is fully implemented (register, login, logout, token refresh with rotation).

---

## 2. Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.5 | Framework |
| Spring Security | (Boot managed) | Auth filter chain, password encoding |
| PostgreSQL | 14+ | Primary database |
| Flyway | (Boot managed) | Database migrations |
| JPA / Hibernate | (Boot managed) | ORM |
| JJWT | 0.12.6 | JWT generation and validation |
| MapStruct | 1.5.5 | DTO ↔ Entity mapping (compile-time, zero runtime cost) |
| Lombok | (Boot managed) | Boilerplate reduction (getters, builders, constructors) |
| Maven | 3.9 | Build tool |
| Docker + Nginx | — | Containerization and reverse proxy |

---

## 3. Prerequisites

- Java 21 (check: `java -version`)
- Maven 3.9+ or use the included `./mvnw` wrapper
- PostgreSQL 14+ running locally, OR Docker Desktop
- An `.env` file at the project root (see below)

---

## 4. Environment Setup

Create a `.env` file at the project root. Never commit this file.

```env
DATASOURCE_URL=jdbc:postgresql://localhost:5432/studentlife
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=yourpassword

JWT_SECRET=your-very-long-random-secret-at-least-64-chars
JWT_ACCESS_TOKEN_EXPIRE=900000        # 15 minutes in ms
JWT_REFRESH_TOKEN_EXPIRE=2592000000   # 30 days in ms

APP_SECURE_COOKIE=false               # true in production (enables Secure + SameSite=None on cookies)
```

**Generating a JWT secret:**
```bash
openssl rand -hex 64
```

---

## 5. Running the App

### Option A — Local Maven (fastest for development)

```bash
# Make sure PostgreSQL is running locally first
./mvnw spring-boot:run
```

App starts on `http://localhost:5000`

### Option B — Docker Compose (full stack with Nginx)

```bash
docker compose up --build
```

- App on port 5000 (internal)
- Nginx on port 80 (HTTP) and 443 (HTTPS)
- Nginx proxies all traffic to `http://app:5000`

### Verifying the app is running

```bash
curl http://localhost:5000/health
# Expected: OK
```

---

## 6. Architecture

### The Core Rule

> **Controllers handle HTTP. Services handle business logic. Never mix them.**

This project follows a strict layered architecture. Each layer has one job:

```
Request → Controller → Service → Repository → Database
                ↑           ↑
           HTTP stuff   Business logic only
```

### Layer Responsibilities

**Controller**
- Reads request body and cookies from `HttpServletRequest`
- Calls the service with plain Java data
- Sets cookies on `HttpServletResponse`
- Wraps the result in `ApiResponse<T>`
- Returns `ResponseEntity`
- Never contains if/else business logic

**Service**
- Receives plain Java types (DTOs, Strings, Longs)
- Applies business rules (validate, compute, orchestrate)
- Calls repositories and other services/utilities
- Returns plain result objects (`AuthResult`, or domain objects)
- Throws `ApiException` for error cases — never returns error flags
- **Never imports anything from `jakarta.servlet.*`**

**Repository**
- Pure Spring Data JPA — only query methods
- No business logic whatsoever

**Entity**
- JPA-mapped domain objects
- Never used directly in API responses (always mapped to a DTO first)

### Where Things Live

| Concern | Layer |
|---|---|
| Read/set cookies | Controller (via `CookieUtil`) |
| Wrap response in `ApiResponse` | Controller |
| Return HTTP status codes | Controller |
| Password hashing | Service |
| Token generation | Service (calls `JWTService`) |
| DB queries | Repository |
| Error handling | Service throws → `GlobalException` catches |
| Request validation (`@Valid`) | Controller annotation, error caught by `GlobalException` |

### Data Flow Example (Login)

```
POST /api/v1/auth/login
        │
        ▼
AuthController.login()
  - reads @RequestBody AuthRequest
  - calls authService.login(request)              ← passes DTO, no HTTP objects
        │
        ▼
AuthServiceImpl.login()
  - finds user by email
  - validates password with BCrypt
  - generates access + refresh tokens
  - saves refresh token hash to DB
  - returns AuthResult(accessToken, refreshToken) ← plain data, no HTTP
        │
        ▼
AuthController.login() (continues)
  - sets accessToken cookie on response
  - sets refreshToken cookie on response
  - wraps in ApiResponse<AuthResponse>
  - returns ResponseEntity.ok(...)
```

---

## 7. Project Structure

```
src/main/java/com/studentlife/studentlifejava/
│
├── Controller/                  # HTTP layer — thin, no business logic
│   ├── AuthController.java      # /api/v1/auth/** endpoints
│   └── healthController.java    # /health endpoint
│
├── Service/                     # Business logic layer
│   ├── AuthService.java         # Interface — defines the contract
│   └── Impl/
│       └── AuthServiceImpl.java # Implementation
│
├── Repository/                  # Data access layer — Spring Data JPA
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   └── RefreshTokenRepository.java
│
├── Entity/                      # JPA database models
│   ├── User.java                # Implements UserDetails (Spring Security)
│   ├── Role.java
│   └── RefreshToken.java
│
├── DTO/                         # Data Transfer Objects
│   ├── AuthResult.java          # Internal result returned from Service → Controller
│   ├── Request/
│   │   ├── AuthRequest.java     # Login request body
│   │   └── RegisterRequest.java # Register request body
│   └── Response/
│       ├── ApiResponse.java     # Universal response wrapper
│       ├── AuthResponse.java    # Login/refresh response body
│       ├── RegisterResponse.java# Register response body
│       └── UserResponse.java    # User data in responses
│
├── JWT/                         # JWT utilities
│   ├── JWTService.java          # Token generation and parsing
│   └── JWTAuthFilter.java       # OncePerRequestFilter — validates JWT on every request
│
├── Security/                    # Spring Security config
│   ├── SecurityConfig.java      # Filter chain, route permissions, beans
│   └── UserDetailService.java   # Loads UserDetails by userId (for JWT validation)
│
├── Exception/                   # Error handling
│   ├── ApiException.java        # Custom exception with HTTP status
│   ├── ErrorsExceptionFactory.java  # Static factory: notFound(), unauthorized(), etc.
│   └── GlobalException.java     # @RestControllerAdvice — catches all exceptions
│
├── Mapper/                      # MapStruct mappers (compile-time code generation)
│   ├── UserMapper.java          # User ↔ UserResponse, RegisterRequest → User
│   ├── RoleMapper.java          # Role → String name
│   └── MapperConfiguration.java # Shared MapStruct config (componentModel = "spring")
│
├── Utils/                       # Utility helpers
│   ├── CookieUtil.java          # Build, set, read, clear HTTP cookies
│   └── TokenHashUtil.java       # SHA-256 hashing for refresh tokens
│
├── script/
│   └── RoleSeeder.java          # Seeds "admin" and "user" roles on startup
│
└── StudentlifejavaApplication.java  # Main entry point
```

```
src/main/resources/
├── application.yaml             # App config — all secrets via env vars
└── db/migration/
    └── V1__init_schema.sql      # Flyway migration — initial schema
```

---

## 8. Database Schema

Flyway manages all schema changes. Never run raw DDL manually. Add new migrations as `V2__description.sql`, `V3__description.sql`, etc.

```
┌──────────────┐         ┌───────────────┐
│    users     │         │     roles     │
│──────────────│         │───────────────│
│ id (PK)      │◄──┐     │ id (PK)       │
│ fullname     │   │     │ name          │
│ username     │   │     │ created_at    │
│ email        │   │     │ updated_at    │
│ password     │   │     └───────────────┘
│ university   │   │              ▲
│ major        │   │              │
│ academic_year│   │     ┌────────────────┐
│ is_active    │   │     │   user_role    │
│ created_at   │   │     │────────────────│
│ updated_at   │   └─────│ user_id (FK)   │
└──────────────┘         │ role_id (FK)   │
        │                └────────────────┘
        │
        ▼
┌──────────────────┐
│  refresh_token   │
│──────────────────│
│ id (PK)          │
│ token_hash       │  ← SHA-256 of the actual token (never stored plain)
│ user_id (FK)     │
│ expired_at       │
│ revoked          │
│ rotated_at       │  ← set when token is rotated (for audit trail)
│ created_at       │
└──────────────────┘
```

**Key design decisions:**
- Refresh tokens are stored as SHA-256 hashes — the raw token is only ever in the HTTP cookie
- `revoked = true` + `rotated_at` set means the token was legitimately rotated (not stolen)
- `revoked = true` + a second refresh attempt = token theft → all user sessions revoked

---

## 9. API Endpoints

Base URL: `http://localhost:5000`

All responses follow this envelope:
```json
{
  "status": 200,
  "success": true,
  "message": "Human readable message",
  "data": { ... }
}
```

### Public endpoints (no auth required)

#### `POST /api/v1/auth/register`

```json
// Request
{
  "fullname": "John Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securepassword"
}

// Response 201
{
  "status": 201,
  "success": true,
  "message": "Registered successfully.",
  "data": {
    "accessToken": "<jwt>",
    "user": {
      "id": 1,
      "fullname": "John Doe",
      "username": "johndoe",
      "email": "john@example.com",
      "university": null,
      "major": null,
      "academicYear": null,
      "isActive": true,
      "roles": ["user"],
      "createdAt": "2026-06-29T00:00:00Z",
      "updatedAt": "2026-06-29T00:00:00Z"
    }
  }
}
// Also sets: accessToken cookie, refreshToken cookie (HTTP-only)
```

#### `POST /api/v1/auth/login`

```json
// Request
{
  "email_or_username": "john@example.com",
  "password": "securepassword"
}

// Response 200
{
  "status": 200,
  "success": true,
  "message": "Login successfully.",
  "data": {
    "accessToken": "<jwt>"
  }
}
// Also sets: accessToken cookie, refreshToken cookie (HTTP-only)
```

#### `POST /api/v1/auth/refresh`

No request body needed. Reads `refreshToken` from cookie automatically.

```json
// Response 200
{
  "status": 200,
  "success": true,
  "message": "Token refreshed successfully.",
  "data": {
    "accessToken": "<new-jwt>"
  }
}
// Also rotates: accessToken cookie, refreshToken cookie
```

#### `POST /api/v1/auth/logout`

No request body needed. Reads `refreshToken` from cookie automatically.

```json
// Response 200
{
  "status": 200,
  "success": true,
  "message": "Logout successfully.",
  "data": null
}
// Also clears: accessToken cookie, refreshToken cookie
```

### Protected endpoints (JWT required)

Send the access token via either:
- Cookie: `accessToken=<jwt>` (set automatically by login/register)
- Header: `Authorization: Bearer <jwt>`

#### `GET /health`

```
// Response 200
OK
```

### Error responses

```json
// 400 — validation failed (@Valid)
{ "status": 400, "success": false, "message": "password must be at least 8 characters", "data": null }

// 401 — unauthorized
{ "status": 401, "success": false, "message": "Refresh token is missing.", "data": null }

// 404 — not found
{ "status": 404, "success": false, "message": "User not found.", "data": null }

// 422 — business rule violation
{ "status": 422, "success": false, "message": "This email or username already been used.", "data": null }

// 500 — unexpected server error
{ "status": 500, "success": false, "message": "Internal server error", "data": null }
```

---

## 10. Authentication Flow

### How JWT works in this app

The app issues two tokens on login/register:

| Token | Stored in | Lifetime | Purpose |
|---|---|---|---|
| Access token | HTTP-only cookie + response body | 15 min | Proves identity on every request |
| Refresh token | HTTP-only cookie only | 30 days | Gets a new access token when it expires |

Both cookies are `httpOnly` (JavaScript cannot read them) which prevents XSS token theft.

### Request authentication (JWTAuthFilter)

Every request passes through `JWTAuthFilter` before reaching any controller:

1. Extract token from `Authorization: Bearer <token>` header **or** `accessToken` cookie
2. Parse the JWT, check signature + expiry + type = `"access"`
3. Load the user from DB via `UserDetailService`
4. Set `SecurityContextHolder` — Spring Security now knows who made the request
5. Pass to controller

### Token refresh flow

When the access token expires (15 min), the frontend calls `POST /api/v1/auth/refresh`. The refresh token cookie is sent automatically by the browser.

The server:
1. Reads `refreshToken` cookie
2. Hashes it with SHA-256
3. Looks up the hash in the DB
4. Validates it is not revoked and not expired
5. **Rotates**: marks old token revoked, issues a brand new refresh token
6. Issues a new access token
7. Sets both new tokens as cookies

### Token theft detection

If a refresh token is used **after it was already rotated** (revoked = true), the server detects a possible theft and immediately revokes **all** refresh tokens for that user. Every session is terminated. The user must log in again.

---

## 11. How to Add a New Feature

Follow these exact steps every time. Example: adding a "get my profile" endpoint.

### Step 1 — Add the repository method (if needed)

```java
// UserRepository.java
Optional<User> findById(Long id);  // already exists in JpaRepository
```

### Step 2 — Create the Service interface method

```java
// UserService.java (new interface)
public interface UserService {
    UserResponse getProfile(Long userId);
}
```

### Step 3 — Implement the service

```java
// UserServiceImpl.java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ErrorsExceptionFactory.notFound("User not found."));
        return userMapper.toUserResponse(user);
    }
}
```

The service returns `UserResponse` — a plain DTO. No HTTP, no `ApiResponse` wrapping.

### Step 4 — Add the controller endpoint

```java
// UserController.java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal User currentUser  // injected by Spring Security
    ) {
        UserResponse profile = userService.getProfile(currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Profile fetched.", profile));
    }
}
```

The controller does one thing: calls the service, wraps the result, returns it.

### Step 5 — Add Flyway migration if DB changed

Create `V2__add_something.sql` in `src/main/resources/db/migration/`. Never edit existing migration files.

### Summary of the pattern

```
Entity (DB model)
  → Repository (query)
    → Service (business logic, returns DTO or plain type)
      → Controller (HTTP in, HTTP out, wraps ApiResponse)
```

---

## 12. Key Rules

**1. Services never touch HTTP**

If you see `HttpServletRequest` or `HttpServletResponse` imported in a service file — it is wrong. Move cookie reading/writing to the controller.

**2. Services throw exceptions, never return error flags**

```java
// CORRECT
throw ErrorsExceptionFactory.notFound("User not found.");

// WRONG — do not do this
return new ApiResponse<>(404, false, "User not found.", null);
```

`GlobalException` catches every `ApiException` and formats the error response automatically.

**3. Entities are never returned from controllers**

Always map `Entity → DTO` (using MapStruct mappers) before returning data. Entities can expose password fields and internal data.

**4. Never edit existing Flyway migrations**

Once a migration file is committed and run, it must never be modified. Add a new `V{n}__description.sql` file instead.

**5. All secrets go in `.env`, never in `application.yaml`**

`application.yaml` only references environment variables via `${VAR_NAME}`.

**6. Cookie behavior by environment**

`CookieUtil` reads `app.secure-cookie` from config:
- `false` (local dev): `httpOnly=true`, `secure=false`, `sameSite=Lax`
- `true` (production): `httpOnly=true`, `secure=true`, `sameSite=None`

Set `APP_SECURE_COOKIE=true` in production `.env`.

**7. Throwing the right exception**

```java
ErrorsExceptionFactory.badRequest("...")    // 400 — malformed input
ErrorsExceptionFactory.unauthorized("...")  // 401 — not authenticated
ErrorsExceptionFactory.forbidden("...")     // 403 — authenticated but not allowed
ErrorsExceptionFactory.notFound("...")      // 404 — resource doesn't exist
ErrorsExceptionFactory.validation("...")    // 422 — business rule violated
ErrorsExceptionFactory.internal("...")      // 500 — unexpected server error
```

---

## Quick Reference

| Task | File to touch |
|---|---|
| Add a new endpoint | `Controller/` + `Service/` |
| Add a new DB table | New `Vn__name.sql` migration + new `Entity/` |
| Change response shape | `DTO/Response/` |
| Add a new query | `Repository/` |
| Change security rules | `Security/SecurityConfig.java` |
| Add a new mapper | `Mapper/` |
| Seed data on startup | `script/` (implements `CommandLineRunner`) |
