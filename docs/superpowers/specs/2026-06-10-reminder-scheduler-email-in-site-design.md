# Reminder Scheduler, Email, and In-Site Notification Design

## Goal

Implement notification delivery that matches the product rule:

- In-site reminders are shown every 30 minutes during available time when the user has unfinished work.
- Email reminders are sent only for due work:
  - unfinished Daily Goals at 22:59 and 23:29, using 23:59 as the daily due time;
  - unfinished Deadline Tasks at 1 hour before and 30 minutes before `deadlineAt`.

The implementation should extend the existing Spring Scheduler, Reminder, ReminderHistory, NotificationPreference, and dashboard reminder UI rather than introduce a separate notification system.

## Current Context

- `ReminderScheduler` already runs scheduled checks.
- `ReminderService` already creates reminders, records history, and applies notification preferences.
- `NotificationPreference` already supports `emailEnabled`, `inSiteEnabled`, and `minimumIntervalMinutes`, with a default interval of 30 minutes.
- `Reminder` already stores channel, type, task, message, scheduled time, sent time, and status.
- The dashboard already fetches `/api/reminders` and renders recent reminders.
- Email is currently a local stub; actual SMTP delivery is not implemented yet.

## Notification Rules

### In-Site Availability Reminders

- Run from the scheduler at a short interval, such as every minute.
- For each user:
  - load notification preferences;
  - require `inSiteEnabled`;
  - calculate availability from personal schedules;
  - continue only if `now` is inside an availability slot;
  - find unfinished Daily Goals and Deadline Tasks;
  - choose the highest-priority unfinished task with the existing `PriorityCalculator`;
  - create and send an `IN_SITE` reminder if no availability in-site reminder was sent for the user in the last 30 minutes.
- These reminders must not send email.

### Daily Goal Email Reminders

- Treat each Daily Goal as due at 23:59 on the current local date.
- If a Daily Goal is not completed today, send email reminders at:
  - 22:59;
  - 23:29.
- The scheduler may run every minute, so each check should match reminders whose target time is between `now` and `now + 1 minute`, then apply a duplicate guard.
- One Daily Goal can receive at most one email for each target offset per day.

### Deadline Task Email Reminders

- For each unfinished Deadline Task, send email reminders at:
  - `deadlineAt - 60 minutes`;
  - `deadlineAt - 30 minutes`.
- Do not send if the task is already done.
- Do not send more than once for the same task and same offset.
- Existing overdue behavior can remain separate, but this feature focuses on the requested 1-hour and 30-minute email reminders.

## Reminder Types

Add explicit reminder types so duplicate prevention is deterministic:

- `AVAILABILITY_IN_SITE`
- `DAILY_GOAL_DAY_END_ONE_HOUR`
- `DAILY_GOAL_DAY_END_THIRTY_MINUTES`
- `DEADLINE_ONE_HOUR`
- `DEADLINE_THIRTY_MINUTES`

Existing types can remain for compatibility, but the new scheduler paths should use the new explicit types. The SQL bootstrap enum list must be updated to include these values.

## Email Delivery

- Add `spring-boot-starter-mail`.
- Add SMTP settings to `application.yml` or profile-specific config using environment variables.
- Add `.env.example` entries for SMTP host, port, username, password, sender, auth, and TLS.
- Introduce a small email sender component used by `ReminderService`.
- On success:
  - mark the reminder as `SENT`;
  - save `ReminderHistory` with a success reason.
- On failure:
  - mark the reminder as `FAILED`;
  - save `ReminderHistory` with the failure reason;
  - keep failures visible through `/api/reminders`.
- For local development, support disabling actual email delivery and recording a skipped or stubbed result if SMTP is not configured.

## Backend Components

- `ReminderScheduler`
  - Run availability in-site checks.
  - Run due email checks.
  - Keep delayed focus in-site reminder processing.

- `ReminderService`
  - Split reminder creation by channel and trigger.
  - Add methods for availability in-site, daily-goal due email, and deadline offset email checks.
  - Keep preference checks and duplicate guards close to reminder creation.
  - Route actual delivery by `AlertChannel`.

- `ReminderRepository`
  - Add query methods needed for deterministic duplicate checks by user/task, reminder type, channel, and scheduled date/time range.

- `DailyGoalRepository`
  - Reuse `findByUserUserId`.
  - Filter unfinished Daily Goals in `ReminderService` with `DailyGoal.isCompletedOn(today)`.

- `MailReminderSender`
  - Build plain text email subject/body.
  - Send through `JavaMailSender`.
  - Return success/failure to `ReminderService`.

## Frontend Components

- Dashboard JavaScript:
  - Continue loading `/api/reminders`.
  - Add periodic polling, for example every 30 seconds.
  - Track already displayed reminder IDs in memory and local storage for the active browser.
  - Show new `IN_SITE` sent reminders as dismissible toast/banner notifications.
  - Keep the existing reminder list as the history view.

- Dashboard HTML/CSS:
  - Add a small toast region with `aria-live="polite"`.
  - Style toasts as compact notifications, not full modal dialogs.

## Error Handling

- If a user disables in-site alerts, skip availability in-site reminders.
- If a user disables email, skip email reminders.
- If no unfinished work exists, do nothing.
- If the user is not currently available, do not create availability in-site reminders.
- If SMTP is not configured in local development, do not crash scheduled jobs.
- If email sending fails, record `FAILED` and continue checking other users.

## Testing

- Backend service tests:
  - creates in-site availability reminder when available and unfinished work exists;
  - blocks duplicate in-site reminders inside 30 minutes;
  - creates Daily Goal email at 22:59 and 23:29 for unfinished goals;
  - does not create Daily Goal email when completed today;
  - creates Deadline Task email at 1 hour and 30 minutes before deadline;
  - does not create duplicate offset emails;
  - records failed email delivery as `FAILED`.

- MVC/page tests:
  - dashboard contains the toast region.

- Build verification:
  - run the Gradle test suite.
  - if a local server is started, verify the dashboard renders and reminder polling does not break existing lists.

## Out of Scope

- WebSocket or Server-Sent Events.
- Browser OS-level Notification API permission flow.
- Rich HTML email templates.
- User-configurable daily due time for Daily Goals.
- Mark-as-read persistence beyond the existing reminder history/list.

## Acceptance Criteria

- In-site reminders are generated only during available time, only for unfinished work, and no more than once per 30 minutes per user.
- Email reminders are generated only at the requested due offsets.
- Daily Goal email timing uses 23:59 as the daily due time.
- Deadline Task email timing uses each task's `deadlineAt`.
- Actual SMTP email delivery is supported when configured.
- Local development does not fail when SMTP is not configured.
- Dashboard users see new in-site reminders without manually refreshing.
