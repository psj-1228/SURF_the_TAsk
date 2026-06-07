# Task CRUD and Progress Page Design

## Scope

Implement the frontend for Daily Goal and Deadline Task CRUD, connect it to the existing Spring Boot REST APIs, and add a separate progress page.

The implementation must follow:

- `HELP.md` project rules.
- `Implement.md` API contracts and frontend verification rules.
- Existing Spring MVC, Thymeleaf, static CSS, and vanilla JavaScript structure.

## User Decisions

- Daily Goal and Deadline Task CRUD happens on the dashboard without page navigation.
- Completion check is included in the dashboard task actions.
- The dashboard keeps a simple progress summary.
- Detailed progress moves to a separate progress page.
- The progress page must make each Daily Goal's streak easy to check.

## Dashboard Design

The existing `/dashboard` page remains the primary task workspace.

Add compact task controls near the current Daily Goal and Deadline Task sections:

- Daily Goal create form.
- Deadline Task create form.
- Inline edit state for each task card.
- Complete button for each incomplete task.
- Delete button with a browser confirmation prompt.

Each task card should show:

- Title.
- Description or fallback metadata.
- Status.
- Estimated minutes.
- Importance.
- Daily Goal streak metadata when available.
- Deadline metadata when available.

After create, update, delete, or completion actions, the dashboard refreshes:

- `GET /api/tasks`
- `GET /api/progress`
- Existing auxiliary dashboard data where needed.

The dashboard keeps a small progress summary using:

- Completion rate.
- Done task count.
- Incomplete task count.
- Best Daily Goal streak.
- A link or button to `/progress`.

## Progress Page Design

Add a Spring MVC route for `/progress` and a matching Thymeleaf template, CSS, and JavaScript.

The page should be a focused progress review screen, not another editing workspace.

Main sections:

- Overall progress summary from `GET /api/progress`.
- Large progress wave visualization based on `completionRate`.
- Daily Goal streak section built from `GET /api/tasks`.
- Priority task section from `progress.priorityTasks`.

The Daily Goal streak section should list every task with `taskType === "DAILY_GOAL"` and show:

- Goal title.
- Current streak.
- Target count per day.
- Last completed date.
- Status.
- Estimated minutes and importance as supporting metadata.

Sort Daily Goals by:

1. Higher `currentStreak`.
2. Recent `lastCompletedDate`.
3. Higher `importance`.

If there are no Daily Goals, show a clear empty state and guide the user back to the dashboard.

## API Contract

All protected requests use:

```http
Authorization: Bearer <jwt>
```

Dashboard task actions:

- `POST /api/tasks/daily-goals`
- `POST /api/tasks/deadline-tasks`
- `GET /api/tasks`
- `PUT /api/tasks/{taskId}`
- `DELETE /api/tasks/{taskId}`
- `POST /api/tasks/{taskId}/completion`

Progress page:

- `GET /api/progress`
- `GET /api/tasks`

The frontend should keep using the existing `surfUser` local storage value that stores the login response and token.

## Error Handling

If any protected API returns `401`, clear `surfUser` and redirect to `/login`.

For validation or server errors:

- Keep the user on the same page.
- Show a concise status message near the relevant task area or page-level status region.
- Prefer backend `message` or `details` fields when available.

## Visual Direction

Keep the existing ocean/dashboard identity from `Implement.md`.

Dashboard controls should stay compact and practical:

- No new landing page.
- No page navigation for CRUD.
- No modal-heavy flow unless inline editing becomes visually cramped.

Progress page should feel more review-oriented:

- Larger wave visualization.
- Clear Daily Goal streak cards or rows.
- Scannable summary metrics.

Cards should use the existing 8px radius style and restrained color palette.

## Verification

Primary verification follows `HELP.md` and `Implement.md`:

```bash
cd implementation
gradle bootRun
```

Manual browser verification targets:

- `http://localhost:8080/login`
- `http://localhost:8080/dashboard`
- `http://localhost:8080/progress`

Verify:

- Login token is reused for protected APIs.
- Daily Goal creation appears on the dashboard.
- Deadline Task creation appears on the dashboard.
- Task update changes the rendered card.
- Task deletion removes the card.
- Completion check updates task status and dashboard progress summary.
- `/progress` loads overall progress and Daily Goal streak data.
- A Daily Goal's streak is visible per goal.
- Unauthorized state redirects to `/login`.

`gradle test` may be used as an additional backend check, but the running MVC/frontend flow is the primary verification target for this work.

## Self-Review

- No placeholder requirements remain.
- Dashboard CRUD scope is separate from progress review scope.
- Daily Goal per-goal streak visibility is explicit.
- API paths match `Implement.md`.
- Verification uses the project-required `gradle bootRun` flow.
