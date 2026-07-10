# Assignment Collaboration — API Reference

Status: **all endpoints below are implemented and verified live** (real DB, real Cloudinary, real
rate-limit testing). See `docs/assignment-collaboration.md` for the design reasoning behind these
endpoints — this doc is just the contract.

## Conventions

- **Base path**: `/api/v1`
- **Auth**: every endpoint below requires `Authorization: Bearer <access token>`. There is no
  anonymous access anywhere in this feature.
- **Envelope**: every response (success or error) is wrapped the same way:
  ```json
  { "status": 200, "success": true, "message": "...", "data": { } }
  ```
  Errors use the same shape with `"success": false` and `"data": null`.
- **Common error statuses**: `400` (bad request / validation), `401` (missing/invalid token), `403`
  (authenticated but not allowed), `404` (not found), `429` (rate limited), `500` (server error).
- **Enums**:
  - `TaskStatus`: `"todo"` | `"progress"` | `"done"`
  - `InviteStatus`: `"PENDING"` | `"ACCEPTED"` | `"DECLINED"`

## Access model

Two permission tiers, enforced by `AssignmentAccessGuard`:
- **Owner or admin only**: update/delete the assignment, send/revoke invites.
- **Any member** (owner, admin, or a user with an `ACCEPTED` invite): everything else — view, tasks,
  checklist, attachments, reorder, toggle-complete, view members.

---

## Assignments

### Create — `POST /assignments`
**Access:** any authenticated user (creates their own assignment).

Request body:
| Field | Type | Required |
|---|---|---|
| `title` | string | yes |
| `subject` | string | no |
| `description` | string | no |
| `dueDate` | ISO instant, must be in the future | yes |
| `courseId` | number | no |

Response `201`: an `AssignmentResponse` (see shape below).

### List — `GET /assignments?page=0&size=10`
**Access:** returns only assignments the current user created (does not yet include assignments
they're an accepted member of).

Response `200`: Spring `Page<AssignmentResponse>` inside `data`:
```json
{
  "content": [ /* AssignmentResponse[] */ ],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

**`AssignmentResponse` shape** (used in list/create/update/complete — no nested tasks):
```json
{
  "id": 1,
  "title": "string",
  "subject": "string|null",
  "description": "string|null",
  "dueDate": "2026-07-17T07:00:00Z",
  "courseId": 1,
  "progress": 50,
  "completed": false,
  "createdById": 1,
  "createdAt": "...",
  "updatedAt": "..."
}
```

### Get one (with tasks) — `GET /assignments/{id}`
**Access:** any member.

Response `200`: `AssignmentDetailResponse` — everything in `AssignmentResponse` plus:
```json
{
  "...": "...(all AssignmentResponse fields)",
  "tasks": [ /* TaskResponse[], see below */ ],
  "invites": ["pending-invitee@example.com"]
}
```
`invites` is the list of currently-**PENDING** invite emails only (accepted/declined invites don't
appear here — accepted ones show up in `/members` instead).

### Update — `PUT /assignments/{id}`
**Access:** owner or admin only.
Request body: same shape as Create. Response `200`: `AssignmentResponse`.

### Delete — `DELETE /assignments/{id}`
**Access:** owner or admin only. Cascades to all of the assignment's tasks, checklist items,
attachments, and invites. Response `200`, `data: null`.

### Toggle complete — `PATCH /assignments/{id}/complete`
**Access:** any member. Flips `completed` — **independent** of `progress` (doesn't require
`progress == 100`, and toggling it doesn't change `progress`). Response `200`: `AssignmentResponse`.

---

## Tasks

**`TaskResponse` shape** (returned by all task endpoints, and nested inside `AssignmentDetailResponse.tasks`):
```json
{
  "id": 1,
  "assignmentId": 1,
  "title": "string",
  "description": "string|null",
  "status": "todo",
  "assigneeIds": [1, 2],
  "checklist": [ /* ChecklistItemResponse[] */ ],
  "attachments": [ /* AttachmentResponse[] */ ],
  "order": 0,
  "createdAt": "...",
  "updatedAt": "..."
}
```

### Create — `POST /assignments/{assignmentId}/tasks`
**Access:** any member. New tasks are appended at the end (`order` = current task count).

Request body:
| Field | Type | Required |
|---|---|---|
| `title` | string | yes |
| `description` | string | no |
| `assigneeIds` | number[] | no — every ID must correspond to an existing user, or the request is rejected with `400` |

Response `201`: `TaskResponse`. Also triggers a progress recalculation on the parent assignment.

### Update — `PUT /tasks/{taskId}`
**Access:** any member. Same body shape as Create (title/description/assigneeIds — **not** status,
use the dedicated status endpoint). Response `200`: `TaskResponse`.

### Delete — `DELETE /tasks/{taskId}`
**Access:** any member. Cascades to the task's checklist items and attachments. Triggers a progress
recalculation. Response `200`, `data: null`.

### Update status — `PATCH /tasks/{taskId}/status`
**Access:** any member.
Request body: `{ "status": "todo" | "progress" | "done" }`.
Response `200`: `TaskResponse`. Triggers a progress recalculation on the parent assignment
(`progress = round(100 * doneCount / totalCount)`).

### Reorder — `PATCH /assignments/{assignmentId}/tasks/reorder`
**Access:** any member.
Request body: `{ "orderedIds": [3, 1, 2] }` — **must be exactly** the full set of task IDs belonging to
this assignment (no missing IDs, no foreign IDs from another assignment), or the request is rejected
with `400`.
Response `200`: `TaskResponse[]`, in their new order.

> **Note on URL shape:** update/delete/status live at the flat `/tasks/{taskId}` (no `assignmentId` in
> the path) — the task's own foreign key is authoritative for which assignment it belongs to. Only
> create and reorder are nested under `/assignments/{assignmentId}/tasks/...`, since those genuinely
> need the assignment in context (create: which assignment to attach to; reorder: the full ordered set
> is scoped to one assignment).

---

## Checklist

**`ChecklistItemResponse` shape:**
```json
{ "id": 1, "taskId": 1, "text": "string", "done": false }
```

### Add — `POST /tasks/{taskId}/checklist`
**Access:** any member. Body: `{ "text": "string" }`. Response `201`: `ChecklistItemResponse`.

### Toggle — `PATCH /checklist/{itemId}/toggle`
**Access:** any member. Body: `{ "done": true }` — an **explicit** value, not an implicit flip (so
retrying the same request is safe). Response `200`: `ChecklistItemResponse`.

### Delete — `DELETE /checklist/{itemId}`
**Access:** any member. Response `200`, `data: null`.

---

## Attachments

**`AttachmentResponse` shape:**
```json
{
  "id": 1,
  "taskId": 1,
  "name": "brief.pdf",
  "size": 204800,
  "url": "https://res.cloudinary.com/.../brief.pdf",
  "uploadedAt": "..."
}
```

### Upload — `POST /tasks/{taskId}/attachments`
**Access:** any member. `Content-Type: multipart/form-data`, field name `file`. Max size **10MB**
(server-enforced; larger uploads are rejected before reaching Cloudinary). Uploads to Cloudinary with
auto resource-type detection. Response `201`: `AttachmentResponse`.

### Delete — `DELETE /attachments/{attachmentId}`
**Access:** any member. Deletes from Cloudinary on a **best-effort** basis (if Cloudinary's API call
fails, it's logged and the database row is still removed) — the DB is always the source of truth for
what's "deleted" from the app's perspective. Response `200`, `data: null`.

---

## Invites & Members

**`InviteResponse` shape:**
```json
{ "id": 1, "assignmentId": 1, "email": "friend@example.com", "status": "PENDING", "createdAt": "..." }
```

**`MemberResponse` shape** (also used by `/users/search`):
```json
{ "id": 1, "name": "Sok Dara", "initials": "SD", "color": "#10B981" }
```
`initials` and `color` are derived at response time (not stored) — initials from the first letters of
the first two words of `fullname` (falls back to `username`), color from hashing the user's ID into a
fixed 8-color palette.

### Invite — `POST /assignments/{assignmentId}/invites`
**Access:** owner or admin only. **Rate limited** (shared 10 req/min per-IP bucket with login/register/
OTP endpoints).
Request body: `{ "email": "friend@example.com" }`.
Rejects with `400` if that email already has a PENDING invite for this assignment. Sends an email via
the app's existing mail service. If the email belongs to an already-registered user, the invite is
linked to them immediately (`invited_user_id`) — but this link is never trusted for access control by
itself; `email` + `status` are the source of truth. Response `201`: `InviteResponse`.

### Revoke — `DELETE /assignments/{assignmentId}/invites/{email}`
**Access:** owner or admin only. Only removes a **PENDING** invite matching that email; `404` if none
exists. Response `200`, `data: null`.

### Members — `GET /assignments/{assignmentId}/members`
**Access:** any member. Returns the assignment owner plus every user with an `ACCEPTED` invite.
Response `200`: `MemberResponse[]`.

> **Not built (separate planned feature):** there is no accept/decline endpoint yet. An invite can only
> reach `ACCEPTED` status through that future feature (or manually, for testing). The schema (`status`,
> `token`) is already in place for it.

---

## User search

### Search by email — `GET /users/search?email=<prefix>`
**Access:** any authenticated user (not assignment-scoped — used for invite autocomplete). **Rate
limited** (same shared bucket as invite-send). Capped at **10 results**, matched by email prefix
(case-insensitive). This endpoint queries across all registered users (unlike everything else in this
feature, which is scoped to one user's own data), so both the cap and the rate limit exist specifically
to prevent it being used to enumerate/scrape the user base.
Response `200`: `MemberResponse[]`.

---

## Known gap: frontend field names

This API uses this codebase's existing naming conventions (`description`, `dueDate`, `createdById`).
The original frontend spec for this feature used different names (`desc`, `deadline`, `owner_id`) —
no renaming was done on the backend to force a match, so the frontend integration layer needs a small
mapping step. Everything else (`subject`, `progress`, `completed`, `status`, task/checklist/attachment
shapes) matches the frontend spec's naming directly.
