# SURF the TAsk Backend Implementation and API Contract

## 1. Implementation Scope

Backend source code lives under `implementation/`.

Implemented layers:

- Spring Boot application scaffold
- JPA Entity model
- Spring Data Repository layer
- Service layer for Auth, Task, Schedule, Focus, Progress, Reminder
- REST Controller layer
- Global error response handling
- Scheduler for availability, deadline, and delayed in-site reminders
- CORS configuration for frontend development

Documentation rule:

- When implementation differs from the Class Diagram, or when the Class Diagram itself changes, record the change in `docs/Extensions.md`.
- For new classes, methods, DTO fields, repository query methods, or development-only entry points, include the class name and reason in `docs/Extensions.md`.

Backend stack:

| Area | Value |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| API | Spring MVC REST |
| Persistence | Spring Data JPA |
| Local DB | MySQL |
| Production DB | Amazon RDS MySQL |
| Security | Spring Security + JWT Bearer token |
| Validation | Jakarta Bean Validation |
| Scheduler | Spring Scheduling |

## 2. Local Runtime

Base URL:

```text
http://localhost:8080
```

Run command from `implementation/` after Java 17 and Gradle are installed:

```bash
gradle bootRun
```

Optional wrapper generation:

```bash
gradle wrapper
./gradlew bootRun
```

Default local MySQL connection:

```text
database: surf_the_task
username: surf_user
password: surf_password
```

Create the local MySQL database and tables:

```powershell
cd implementation
.\scripts\setup-mysql.ps1 -AdminUser root -AdminPassword "<mysql-root-password>"
```

The setup script executes:

- `src/main/resources/db/mysql/01-create-database.sql`
- `src/main/resources/db/mysql/02-create-tables.sql`

Local profile:

```text
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:mysql://localhost:3306/surf_the_task?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=surf_user
DB_PASSWORD=surf_password
JWT_SECRET=replace-with-at-least-32-bytes-random-secret
JWT_EXPIRATION_MINUTES=1440
```

AWS RDS production profile:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<rds-endpoint>:3306/surf_the_task?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<rds-user>
DB_PASSWORD=<rds-password>
JWT_SECRET=<strong-random-secret>
JWT_EXPIRATION_MINUTES=1440
```

## 3. Common Rules

All API paths start with:

```text
/api
```

Content type:

```http
Content-Type: application/json
```

Date and time format:

```text
LocalDateTime: 2026-06-07T14:30:00
LocalDate: 2026-06-07
LocalTime: 14:30:00
```

Enums:

| Enum | Values |
|---|---|
| `TaskStatus` | `TODO`, `IN_PROGRESS`, `DONE` |
| `ReminderType` | `AVAILABILITY_BASED`, `DEADLINE_WARNING`, `OVERDUE_ALERT`, `DELAYED_IN_SITE` |
| `ReminderStatus` | `PENDING`, `SENT`, `FAILED`, `SKIPPED`, `CANCELED` |
| `AlertChannel` | `EMAIL`, `IN_SITE` |
| `RepeatType` | `NONE`, `DAILY`, `WEEKLY` |
| `DayOfWeek` | `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY` |

Authentication note:

- `POST /api/auth/login` returns a signed JWT in `token`.
- Frontend must send the token on protected APIs:

```http
Authorization: Bearer <jwt>
```

- User-scoped APIs no longer accept `userId` in the URL.
- The backend identifies the current user from the JWT subject.
- Resource URLs such as `/api/tasks/{taskId}` still include resource IDs, but the service layer verifies that the resource belongs to the authenticated user.
- Legacy `/api/users/{userId}/...` API routes are not part of the contract.

## 4. Error Response

All handled errors return this shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/auth/register",
  "timestamp": "2026-06-07T14:30:00",
  "details": [
    "email: must be a well-formed email address"
  ]
}
```

Common status codes:

| Status | Code |
|---|---|
| `400` | `BAD_REQUEST`, `VALIDATION_ERROR` |
| `401` | `UNAUTHORIZED` |
| `403` | `FORBIDDEN` |
| `404` | `NOT_FOUND` |
| `409` | `DUPLICATE_RESOURCE` |
| `500` | `INTERNAL_ERROR` |

## 5. Auth API

### Register

```http
POST /api/auth/register
```

Request:

```json
{
  "loginId": "psj",
  "password": "1234",
  "name": "박성준",
  "email": "psj@example.com"
}
```

Response `201`:

```json
{
  "userId": 1,
  "loginId": "psj",
  "name": "박성준",
  "email": "psj@example.com",
  "createdAt": "2026-06-07T14:30:00"
}
```

### Login

```http
POST /api/auth/login
```

Request:

```json
{
  "loginId": "psj",
  "password": "1234"
}
```

Response `200`:

```json
{
  "userId": 1,
  "loginId": "psj",
  "name": "박성준",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Logout

```http
POST /api/auth/logout
```

Headers:

```http
Authorization: Bearer <jwt>
```

Response:

```text
204 No Content
```

## 6. Task API

Task response shape:

```json
{
  "taskId": 10,
  "userId": 1,
  "taskType": "DEADLINE_TASK",
  "title": "Open Source HW",
  "description": "Finish backend",
  "estimatedMinutes": 120,
  "importance": 5,
  "status": "TODO",
  "deadlineAt": "2026-06-10T23:59:00",
  "warningThresholdHours": 24,
  "targetCountPerDay": null,
  "currentStreak": null,
  "lastCompletedDate": null,
  "createdAt": "2026-06-07T14:30:00",
  "updatedAt": "2026-06-07T14:30:00"
}
```

### Create Daily Goal

```http
POST /api/tasks/daily-goals
```

Request:

```json
{
  "title": "Algorithm study",
  "description": "Solve one problem",
  "estimatedMinutes": 60,
  "importance": 4,
  "targetCountPerDay": 1
}
```

Response:

```text
201 TaskResDto
```

### Create Deadline Task

```http
POST /api/tasks/deadline-tasks
```

Request:

```json
{
  "title": "Open Source SW design",
  "description": "Implement backend APIs",
  "estimatedMinutes": 180,
  "importance": 5,
  "deadlineAt": "2026-06-10T23:59:00",
  "warningThresholdHours": 24
}
```

Response:

```text
201 TaskResDto
```

### Get Tasks

```http
GET /api/tasks
```

Response:

```json
[
  {
    "taskId": 10,
    "userId": 1,
    "taskType": "DEADLINE_TASK",
    "title": "Open Source SW design",
    "description": "Implement backend APIs",
    "estimatedMinutes": 180,
    "importance": 5,
    "status": "TODO",
    "deadlineAt": "2026-06-10T23:59:00",
    "warningThresholdHours": 24,
    "targetCountPerDay": null,
    "currentStreak": null,
    "lastCompletedDate": null,
    "createdAt": "2026-06-07T14:30:00",
    "updatedAt": "2026-06-07T14:30:00"
  }
]
```

### Get Incomplete Tasks

```http
GET /api/tasks/incomplete
```

Response:

```text
200 TaskResDto[]
```

### Update Task

```http
PUT /api/tasks/{taskId}
```

Request:

```json
{
  "title": "Open Source SW design",
  "description": "Finish service and controller",
  "estimatedMinutes": 150,
  "importance": 5,
  "status": "IN_PROGRESS",
  "targetCountPerDay": null,
  "deadlineAt": "2026-06-10T23:59:00",
  "warningThresholdHours": 12
}
```

Notes:

- For `DAILY_GOAL`, `targetCountPerDay` can be updated.
- For `DEADLINE_TASK`, `deadlineAt` and `warningThresholdHours` can be updated.
- Irrelevant subtype fields can be omitted or set to `null`.

Response:

```text
200 TaskResDto
```

### Complete Task

```http
POST /api/tasks/{taskId}/completion
```

Request to complete:

```json
{
  "cancel": false
}
```

Request to cancel latest completion:

```json
{
  "cancel": true
}
```

Response:

```json
{
  "recordId": 1,
  "taskId": 10,
  "completedAt": "2026-06-07T14:30:00",
  "completedDate": "2026-06-07",
  "canceled": false
}
```

### Delete Task

```http
DELETE /api/tasks/{taskId}
```

Response:

```text
204 No Content
```

## 7. Schedule API

### Create Schedule

```http
POST /api/schedules
```

Request:

```json
{
  "title": "Class",
  "dayOfWeek": "MONDAY",
  "startTime": "09:00:00",
  "endTime": "12:00:00",
  "repeatType": "WEEKLY"
}
```

Response `201`:

```json
{
  "scheduleId": 1,
  "userId": 1,
  "title": "Class",
  "dayOfWeek": "MONDAY",
  "startTime": "09:00:00",
  "endTime": "12:00:00",
  "repeatType": "WEEKLY",
  "durationMinutes": 180
}
```

### Get Schedules

```http
GET /api/schedules
```

Response:

```text
200 ScheduleResDto[]
```

### Update Schedule

```http
PUT /api/schedules/{scheduleId}
```

Request:

```json
{
  "title": "Class",
  "dayOfWeek": "MONDAY",
  "startTime": "10:00:00",
  "endTime": "12:00:00",
  "repeatType": "WEEKLY"
}
```

Response:

```text
200 ScheduleResDto
```

### Delete Schedule

```http
DELETE /api/schedules/{scheduleId}
```

Response:

```text
204 No Content
```

### Get Availability

```http
GET /api/availability
```

Response:

```json
[
  {
    "dayOfWeek": "MONDAY",
    "startTime": "00:00:00",
    "endTime": "09:00:00",
    "durationMinutes": 540
  }
]
```

## 8. Focus API

### Start Focus

```http
POST /api/focus-sessions
```

Request:

```json
{
  "taskId": 10
}
```

Response `201`:

```json
{
  "sessionId": 1,
  "userId": 1,
  "taskId": 10,
  "startAt": "2026-06-07T14:30:00",
  "endAt": null,
  "active": true,
  "actualFinished": null,
  "durationMinutes": 0
}
```

### Finish Focus

```http
PATCH /api/focus-sessions/{sessionId}/finish
```

Request when task is really finished:

```json
{
  "actualFinished": true,
  "completeTask": true
}
```

Request when user is not actually finished:

```json
{
  "actualFinished": false,
  "completeTask": false
}
```

Behavior:

- `actualFinished=true` ends the focus session.
- `completeTask=true` also marks the task as `DONE`.
- `actualFinished=false` keeps the session active and schedules a delayed in-site reminder after 5 minutes.

Response:

```text
200 FocusSessionResDto
```

## 9. Progress API

### Get Progress

```http
GET /api/progress
```

Response:

```json
{
  "userId": 1,
  "totalTasks": 5,
  "doneTasks": 2,
  "incompleteTasks": 3,
  "completionRate": 40.0,
  "bestDailyGoalStreak": 3,
  "totalFocusMinutes": 155,
  "completedGoalCount": 2,
  "currentWeekCompletionRate": 60.0,
  "previousWeekCompletionRate": 40.0,
  "weeklyCompletionRateDelta": 20.0,
  "dailyCompletionRates": [
    {
      "date": "2026-06-02",
      "label": "6/2",
      "completedCount": 1,
      "totalTasks": 5,
      "completionRate": 20.0
    }
  ],
  "priorityTasks": []
}
```

`dailyCompletionRates` contains the past seven days including today. `currentWeekCompletionRate` uses the same seven-day window, `previousWeekCompletionRate` uses the seven days before that, and `weeklyCompletionRateDelta` is `current - previous`. `priorityTasks` contains up to 5 incomplete `TaskResDto` objects sorted by importance, deadline urgency, and estimated time.

## 10. Reminder API

### Get Reminders

```http
GET /api/reminders
```

Response:

```json
[
  {
    "reminderId": 1,
    "userId": 1,
    "taskId": 10,
    "focusSessionId": null,
    "reminderType": "DEADLINE_WARNING",
    "channel": "EMAIL",
    "message": "Deadline is near in 12 hours: Open Source SW design",
    "scheduledAt": "2026-06-07T14:30:00",
    "sentAt": "2026-06-07T14:30:00",
    "status": "SENT",
    "resultReason": null
  }
]
```

### Update Notification Preference

```http
PATCH /api/notification-preference
```

Request:

```json
{
  "emailEnabled": true,
  "inSiteEnabled": true,
  "availabilityReminderEnabled": true,
  "deadlineReminderEnabled": true,
  "minimumIntervalMinutes": 30
}
```

Response:

```json
{
  "preferenceId": 1,
  "userId": 1,
  "emailEnabled": true,
  "inSiteEnabled": true,
  "availabilityReminderEnabled": true,
  "deadlineReminderEnabled": true,
  "minimumIntervalMinutes": 30
}
```

## 11. Scheduler Behavior

The backend runs three scheduled checks:

| Scheduler | Interval | Behavior |
|---|---:|---|
| Availability reminder | 60s | If current time is available and incomplete tasks exist, creates and sends an email reminder stub. |
| Deadline reminder | 60s | Sends deadline warning or overdue reminder stubs for incomplete deadline tasks. |
| Delayed in-site alert | 30s | Sends due in-site alerts created when focus was not actually finished. |

Notification sending is currently a local stub:

- `Reminder.status` becomes `SENT`.
- `Reminder.sentAt` is set.
- `ReminderHistory` records the result.
- No real external email provider is called yet.

## 12. Frontend Integration Checklist

Recommended frontend flow:

1. Call `POST /api/auth/register` or `POST /api/auth/login`.
2. Store returned `token` in frontend state, memory, or browser storage.
3. Send `Authorization: Bearer <jwt>` on every protected API request.
4. Render task list from `GET /api/tasks`.
5. Render dashboard from `GET /api/progress`.
6. Use focus APIs for On/Off mode.
7. Poll `GET /api/reminders` or connect a later real-time channel.

Frontend routing note:

- Use `/dashboard` for the authenticated dashboard screen.
- Do not place `userId` in frontend route URLs for user-scoped data.
- `userId` in responses is display/debug metadata only; it is not an authorization boundary.

Suggested frontend types:

```ts
type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";
type TaskType = "DAILY_GOAL" | "DEADLINE_TASK";
type ReminderStatus = "PENDING" | "SENT" | "FAILED" | "SKIPPED" | "CANCELED";
type ReminderType = "AVAILABILITY_BASED" | "DEADLINE_WARNING" | "OVERDUE_ALERT" | "DELAYED_IN_SITE";
type AlertChannel = "EMAIL" | "IN_SITE";
type RepeatType = "NONE" | "DAILY" | "WEEKLY";
```

## 13. Build and Frontend Verification

Frontend and Spring MVC screen work must be verified through the running application.

Primary verification command from `implementation/`:

```bash
gradle bootRun
```

Verification targets:

- Authentication screen: `http://localhost:8080/login`
- Registration screen: `http://localhost:8080/register`
- User dashboard after login: `http://localhost:8080/dashboard`
- Mobile phone viewport for every changed frontend/MVC screen

Rules:

- Use `gradle bootRun` as the primary verification method for frontend/MVC work.
- Use the browser against the running app to verify actual page rendering, API calls, redirects, and console errors.
- For dashboard, progress, task, schedule, focus, and reminder screens, sign in through the ordinary login or registration flow before navigating to authenticated pages.
- Use `/login` and `/register` as verification entry points only when those authentication screens or the authentication flow changed.
- Always verify mobile phone rendering after changing frontend templates, CSS, or frontend JavaScript.
- Mobile verification must confirm that text, form fields, buttons, cards, and primary images are not overlapped, clipped, or horizontally overflowing.
- `gradle test` may be used as an additional backend/context check, but it is not the primary frontend/MVC verification command.
- If the system `gradle` command is unavailable, use an existing Gradle wrapper if present. If no wrapper exists, use a temporary local Gradle distribution or clearly report that Gradle is missing.
- Do not repeatedly try unrelated build commands after Gradle availability has already been diagnosed.

## 14. Frontend 구축 방식

Frontend는 `images/Proto Type.png`의 시각 방향을 기준으로 구축한다. 전체 디자인 포인트는 사용자가 할 일을 단순히 체크하는 것이 아니라, **Task를 서핑하듯이 흐름을 타고 진행한다**는 느낌을 주는 것이다. 따라서 화면은 학습/업무 도구처럼 명확하고 읽기 쉬워야 하지만, 바다, 파도, 서핑, 항해 이미지를 기능별 시각 은유로 적극 활용한다.

### 14.1 Visual Identity

- 핵심 컨셉: `Surf your task`, `Task를 서핑한다`, `가용 시간이라는 파도를 타고 목표를 수행한다`.
- 기본 분위기: 밝은 해변, 맑은 하늘, 바다색, 흰 카드 UI, 네이비 사이드바, 민트/블루 포인트 컬러를 조합한다.
- 브랜드 첫 화면: 로그인/회원가입 화면은 `Proto Type.png`처럼 서핑보드, 해변, 파도 이미지를 사용해 서비스 정체성이 바로 보이게 한다.
- 앱 내부 화면: 기능 화면은 과도한 장식보다 대시보드형 정보 구조를 우선하고, 파도/항해 요소는 배경, 진행률, 상태 표현에 자연스럽게 녹인다.
- 주요 색상 방향:
  - Deep navy: 사이드바, 집중 모드 배경, 야간 항해 화면
  - Ocean blue: 주요 버튼, 선택 상태, 링크
  - Mint/green: 가용 시간, 완료, Focus On 상태
  - Coral/red: 마감 임박, 종료 확인, 위험 동작
  - White/light gray: 카드, 입력 폼, 표/캘린더 배경

### 14.2 MVC 단위 화면 구축 순서

Frontend는 Spring MVC 화면 단위로 나누어 점진적으로 구현한다.

1. 인증 MVC: 로그인, 회원가입 화면
2. 대시보드 MVC: 오늘의 요약, 매일 목표, 마감 업무, 우선순위, 가용 시간
3. 개인 시간표 MVC: 주간 시간표, 일정 추가/수정 폼, 가용 시간 표시
4. Task MVC: Daily Goal, Deadline Task 등록/수정/삭제/완료 처리
5. Focus MVC: Focus On/Off, 집중 타이머, 종료 확인, 지연 알림
6. Progress MVC: 월별/주간/일간 수행률, streak, 완료율, 우선순위 변화
7. Reminder MVC: 알림 목록, 알림 설정, In-site 알림 표시

각 MVC 단위는 `Controller -> View Template -> Static CSS/JS -> REST API 연동` 순서로 구현한다. REST API 계약은 본 문서의 `/api/**` 명세를 따른다.

### 14.3 Focus On/Off Screen Direction

Focus 화면은 **요트를 타고 바다를 항해하는 화면**을 핵심 비주얼로 사용한다. 사용자가 Focus On을 누르면 선택한 Task를 향해 항해를 시작하는 느낌을 주고, Focus Off는 항해를 멈추거나 목적지에 도착하는 행위처럼 표현한다.

- Focus On 상태:
  - 어두운 바다색 또는 석양/밤바다 계열 배경을 사용한다.
  - 중앙에는 타이머와 현재 집중 중인 Task를 크게 배치한다.
  - 요트 또는 항해 경로 이미지를 배경/장면 요소로 배치해 몰입감을 준다.
  - 진행 시간은 원형 게이지, 항로 선, 또는 바다 위 진행 표시로 표현할 수 있다.
- Focus Off 확인:
  - `정말 항해를 마치겠습니까?`처럼 항해 은유를 유지하되, 기능 의미가 흐려지지 않도록 버튼 문구는 명확하게 둔다.
  - 실제 종료가 아니면 5분 후 다시 In-site 알림을 보내는 흐름을 `잠시 표류`, `다시 항로로 복귀` 같은 가벼운 시각 표현으로 연결할 수 있다.
- 금지 방향:
  - 요트/바다 요소가 타이머, Task 제목, 종료 버튼의 가독성을 방해하면 안 된다.
  - 게임 화면처럼 과하게 장식하지 말고, 집중 도구의 차분함을 유지한다.

### 14.4 Monthly Progress Wave Direction

월별 수행률 화면은 **파도 형태의 시각화**를 사용한다. 수행률이 높을수록 파도가 높고 선명하게 보이며, 낮은 날은 잔잔하거나 낮은 파도로 표현한다.

- 월간 캘린더 또는 월간 차트에서 각 날짜의 완료율을 파도 높이, 채도, 물결 개수로 표현한다.
- `완료율`, `연속 기록`, `총 집중 시간`, `완료한 목표 수`는 기존 대시보드 카드처럼 명확한 숫자로 함께 제공한다.
- 파도 시각화는 보조 표현이며, 정확한 값은 tooltip, label, summary card로 확인 가능해야 한다.
- 월별 추세는 선 그래프 대신 물결형 area chart 또는 wave bar chart로 표현할 수 있다.
- 완료율 0%는 잔잔한 얕은 물결, 100%는 가장 높은 파도 또는 밝은 crest로 표현한다.

### 14.5 Interaction Principles

- 사용자는 현재 어떤 흐름에 있는지 즉시 알아야 한다: 로그인, 오늘의 항해, Focus 항해, 월별 파도 기록.
- 버튼과 폼은 명확한 동작명을 사용한다. 시각 은유는 분위기를 만들고, 기능 문구는 혼동 없이 직접적으로 작성한다.
- 화면 전환은 `로그인 -> 대시보드 -> Task/시간표 -> Focus -> Progress` 흐름이 끊기지 않게 구성한다.
- 모바일에서도 사이드바, 카드, 차트, 타이머가 겹치지 않아야 한다.
- API 오류는 사용자가 다음 행동을 알 수 있게 짧고 구체적으로 표시한다.
