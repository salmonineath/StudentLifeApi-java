# Assignment Collaboration — What's Been Built (Stages 1-4)

This covers everything built so far for the Assignment Collaboration feature: schema, entities,
services, controllers, and the reasoning behind the trickier decisions. Stages 5-6 (invites/members/
search, and hardening) aren't covered yet — this doc will be extended once those land.

## The shape of the feature

An `Assignment` now has `Task`s. Each `Task` has a `status` (todo/progress/done), can have multiple
assignees, and can have `ChecklistItem`s and file `Attachment`s. An assignment's `progress` percentage
is derived from how many of its tasks are `done`.

```
Assignment (1) ── (many) Task ── (many) ChecklistItem
                       │
                       └── (many) Attachment
                       └── (many-to-many) Users  [assignees]
```

## Stage 1 — Schema and entities

**Migration `V4__assignment_collaboration.sql`** added the `tasks`, `task_assignees`, `checklist_items`,
`attachments`, and `assignment_invites` tables, plus `subject`/`progress`/`completed` columns on
`assignments`. `V5__add_attachment_resource_type.sql` later added one more column (see Stage 4 below).

Every foreign key column got an index (`idx_tasks_assignment_id`, `idx_checklist_items_task_id`, etc.).
**Why this matters even for a small app:** without an index, looking up "all tasks for this
assignment" forces Postgres to scan the entire `tasks` table row by row. An index turns that into a
direct lookup. It costs a little extra write time on insert, but read speed is what actually matters
here since reads (viewing an assignment) happen far more often than writes.

One deliberate choice: **`Assignment` has no `@OneToMany` back-reference to its tasks or invites.**
It would be convenient to write `assignment.getTasks()`, but that hides a real query behind what looks
like a free field access, and it's easy to accidentally trigger it in a loop (an N+1 problem — see
Stage 6 notes). Instead, everywhere that needs an assignment's tasks calls
`taskRepository.findByAssignmentOrderBySortOrderAsc(assignment)` explicitly, so it's obvious in the
code where a query is happening.

## Stage 2 — Assignment gets subject/progress/completed

Straightforward additions to `Assignment`, `AssignmentRequest`, and `AssignmentResponse`. The one
interesting piece: `PATCH /api/v1/assignments/{id}/complete`. This toggles `completed` **independently**
of `progress` — you can mark an assignment complete even if only 60% of its tasks are done, and it
won't force progress to 100%. That was a deliberate product decision: sometimes "done" and "100% of
tasks checked off" aren't the same thing.

## Stage 3 — Tasks, assignees, reorder, and where progress comes from

**`AssignmentAccessGuard`** (`security/AssignmentAccessGuard.java`) is the key new piece here. It
replaces the old inline owner-check that used to live inside `AssignmentServiceImpl`. It has two
methods:

- `requireOwnerOrAdmin(assignment, user)` — for destructive/administrative actions (delete the
  assignment, manage invites).
- `requireMember(assignment, user)` — for everything collaborative (view, edit tasks, toggle complete).
  A "member" is the owner, an admin, **or** anyone with an `ACCEPTED` invite row for that assignment.

Right now nothing can actually reach `ACCEPTED` status yet (invite accept/decline is Stage 5+), so in
practice `requireMember` currently behaves like "owner or admin" too — but the code is already correct
for when invites exist, without needing to touch this class again.

**Progress is denormalized**, not computed on the fly. `Assignment.progress` is a real column, and
`AssignmentService.recalculateProgress(id)` — `round(100 * doneCount / totalCount)` — runs every time a
task is added, deleted, or has its status changed (see `TaskServiceImpl`). The alternative would be
computing progress with a live aggregate query every time an assignment is fetched, which gets slower
as tasks pile up. Storing it means reads are cheap; the cost of computing it is paid once, at write time.

**Reorder** (`PATCH /assignments/{id}/tasks/reorder`) takes `{orderedIds: [...]}` and is deliberately
paranoid: it loads every task that actually belongs to the assignment, and rejects the request with a
400 unless `orderedIds` is *exactly* that same set — no missing tasks, no IDs from a different
assignment sneaking in. Without that check, a buggy or malicious request could silently detach a task's
ordering from reality.

**URL shape note:** task update/delete/status-change live at the flat `/api/v1/tasks/{taskId}` rather
than nested under `/assignments/{assignmentId}/tasks/{taskId}`. The task ID alone is enough to find its
parent assignment (via `task.getAssignment()`), and nesting would create two sources of truth for
"which assignment does this belong to" — the URL's `assignmentId` and the task's actual FK — that could
disagree if someone passed the wrong one.

## Stage 4 — Checklist items and Cloudinary attachments

Checklist CRUD (`ChecklistItemController`) is the simplest piece in the whole feature — no surprises,
same guard pattern as tasks. One notable choice: toggling a checklist item takes an explicit
`{"done": true}` body rather than just "flip whatever it currently is." An explicit value is
**idempotent** — sending the same request twice has the same effect both times. An implicit flip isn't:
if a network retry resends the request, you'd toggle it twice and end up back where you started,
silently wrong.

**Attachments** go through Cloudinary rather than local disk, matching the same provider the app already
plans to use for avatar uploads. `CloudinaryService` is a thin wrapper with two methods:

- `upload(file)` — uploads with `resource_type: auto`, letting Cloudinary detect whether it's an
  image, video, or generic ("raw") file, and returns the URL, Cloudinary's `public_id`, and the
  detected `resource_type`.
- `delete(publicId, resourceType)` — **best-effort**: if the Cloudinary API call fails, it logs a
  warning and moves on rather than blocking the delete. The database row is always removed regardless.
  An orphaned file sitting in Cloudinary storage is a much smaller problem than a user being unable to
  delete an attachment because a third-party API had a bad moment.

The `resourceType` matters more than it looks: Cloudinary's delete API needs to know whether a resource
is `image`, `video`, or `raw` to find it. Since upload uses `auto` detection, we don't know the type
until *after* uploading — so it gets stored on the `Attachment` row (`V5` migration) specifically so
delete can use the correct value later, instead of guessing.

**Verified live**: uploaded a real file, confirmed it was reachable via its Cloudinary URL, deleted it
through the API, and confirmed via Cloudinary's Admin API that the resource was actually gone from
their storage (the public CDN URL kept returning 200 for a bit afterward — that's just normal edge-cache
lag, not a bug in the delete logic).

## Stage 5 — Invites, members, and user search

**Why `AssignmentAccessGuard.requireMember` finally does something real.** Up through Stage 4, nobody
could ever actually become an `ACCEPTED` invite (no accept endpoint exists — that's intentionally a
separate, not-yet-built feature), so `requireMember` behaved just like "owner or admin." Now that
invites actually get created, a user with an `ACCEPTED` row genuinely gets member access: they can view
the assignment, add/edit tasks, checklist items, and attachments — but **not** send or revoke invites,
or delete the assignment (those stay owner/admin-only via `requireOwnerOrAdmin`). This was verified live
by registering a second user, inviting them, manually flipping their invite row to `ACCEPTED` in the
database (since the accept/decline endpoint itself is out of scope here), and confirming: they could
view and add tasks (200/201), but got a 403 trying to send an invite.

**Why the invite table stores `invited_user_id` even though nobody can accept yet.** At invite-send
time, the code already looks up whether the invited email belongs to a registered user and stores that
FK if so. This is deliberately a *convenience pointer, not a security decision* — `email` + `status`
remain the source of truth for anything access-related. The reason: an invite can be sent to someone who
hasn't signed up yet (`invited_user_id` stays null until they register), and when the accept/decline
feature is eventually built, it re-resolves this by matching the *authenticated* user's email against
the invite's `email` — never by trusting whatever `invited_user_id` happened to hold at send-time.

**Why `/users/search` needed the rate limiter upgraded first.** The existing `RateLimitFilter` matched
request paths with exact string equality (`Set<String>`), which only works for static paths like
`/auth/login`. The invite-send endpoint is `/assignments/{id}/invites` — the `{id}` varies per request,
so exact-match can never catch it. Swapped the matcher to Spring's `AntPathMatcher` so patterns like
`/api/v1/assignments/*/invites` work, while the existing static paths keep working unchanged (a literal
path is a valid, trivially-matching pattern too).

**Why `/users/search` is rate-limited and result-capped at all.** Every other endpoint in this feature
scopes its data to "things one user owns or was invited to" — the data set size tracks *that user's*
activity, not the platform's. `/users/search` is different: it queries across *every* registered user,
so its cost (and its abuse potential — this is exactly the shape of an email-enumeration/scraping
vector) grows with total platform size, not per-user activity. Hence the `LIMIT 10` and the shared rate
limit, verified live: 11 rapid requests, and the requests past the shared per-IP quota all came back 429.

**Member `color`/`initials` are computed, never stored.** Both `/members` and `/users/search` derive
initials from `fullname` (first letter of the first two words) and pick a color by hashing the user's ID
into a fixed palette, at response-build time. Storing either would mean keeping it in sync if a user
renames themselves — computing it fresh means there's nothing to go stale.

## Stage 6 — Fixing the N+1, and a final authorization sweep

**The N+1 was real, and here's exactly where it lived.** `AssignmentServiceImpl.toTaskResponse()` and
`TaskServiceImpl.toResponse()` each queried `checklistItemRepository.findByTaskOrderByIdAsc(task)` and
`attachmentRepository.findByTask(task)` — fine for a single task, but both were being called inside a
`.map(...)` over a *list* of tasks (building `GET /assignments/{id}`'s task list, and the response of
`reorder()`). That's 2 extra queries *per task* — 20 extra queries for a 10-task assignment, and it gets
worse as tasks pile up, not better.

**The fix:** added `findByTaskInOrderByIdAsc(List<Task>)` and `findByTaskIn(List<Task>)` to the two
repositories — one query fetching checklist items (or attachments) for *every* task in the list at once,
using SQL's `IN (...)`. Then group the results into a `Map<Long taskId, List<...>>` in memory, and look
each task's slice up from the map instead of querying per task. Same approach applied in both places that
build a list of `TaskResponse` (`AssignmentServiceImpl.toDetailResponse` and the new
`TaskServiceImpl.toResponses`) — the single-task builders (`create`, `update`, `updateStatus`) were left
as direct per-task queries, since building one `TaskResponse` only ever needs 2 queries regardless of how
many tasks exist elsewhere — that's not an N+1, there's nothing to batch.

**Verified with real SQL logging** (`SPRING_JPA_SHOW_SQL=true`, temporary — not left in `application.yaml`):
seeded one assignment with 5 tasks, 2 checklist items each, then fetched `GET /assignments/{id}` and
counted every `select` fired. Result: **7 total queries** — one each for the current user, the
assignment, the task list, the invites, and three single `IN (...)` / batched queries covering
checklist items, attachments, and task assignees across *all 5 tasks at once* (that last one is
Hibernate's own default collection batching kicking in). Add a 6th task and this stays at 7 — the count
is flat with respect to task count, which is the actual point of fixing an N+1: it's not about the
number 7, it's about the *shape* of the growth curve.

**Authorization sweep:** re-checked every controller written across Stages 3-5 against the
owner-vs-member split, cross-referencing against what was already verified live: `AssignmentController`
(create/list need no assignment-scoped guard since they're either brand-new or already own-data-only;
`get`/`toggleComplete` → `requireMember`; `update`/`delete` → `requireOwnerOrAdmin`), `TaskController` and
`ChecklistItemController` and `AttachmentController` (everything → `requireMember`, since editing content
is the collaborative part), `AssignmentInviteController` (`invite`/`revoke` → `requireOwnerOrAdmin`,
`members` → `requireMember`), `UserSearchController` (no assignment scope — just requires
authentication). No gaps found; this matches what was already exercised live in the Stage 5 test (member
could view/edit but not invite, got a clean 403).

## Where things stand

| Endpoint | Status |
|---|---|
| Assignment CRUD + `/complete` | Done |
| Task CRUD + status + reorder | Done |
| Checklist CRUD | Done |
| Attachment upload/delete (Cloudinary) | Done |
| Invites, `/members`, `/users/search` | Done |
| N+1 query fixes, final authorization pass | Done |

## What's still out of scope

- **Invite accept/decline endpoints** — deliberately not built here (separate planned feature). The
  schema (`status`, `token`, `invited_user_id`) is ready for it.
- **Frontend field-name mapping** — the frontend spec uses `desc`/`deadline`/`owner_id`; this backend
  uses `description`/`dueDate`/`createdById` (matching this codebase's existing naming). The frontend
  integration layer needs to map between them — no backend field renaming was done to force a match.

## Things worth understanding, not just knowing

- **Why a guard class instead of checks inline in every service?** Five different services
  (`AssignmentService`, `TaskService`, `ChecklistItemService`, `AttachmentService`, and later the invite
  service) all need the same "is this person allowed to touch this assignment" logic. Writing it once in
  `AssignmentAccessGuard` means a future change to the authorization rule (e.g. "editors can't delete
  attachments") happens in one place instead of five.
- **Why store `progress` instead of computing it?** This was a direct answer to a scale question: if a
  user has 100+ assignments, recomputing an aggregate over all their tasks every time the list loads
  gets slower as data grows. Storing the answer and updating it on write means reads stay fast
  regardless of how much history piles up.
- **Why does `Task` have no back-reference from `Assignment`?** It's a small thing, but it's the
  difference between "every query in this codebase is visible in the code" and "some queries happen
  invisibly when you access a field." The former is much easier to reason about once the codebase grows.
