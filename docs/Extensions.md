# Class Diagram Extensions 관리 기준

## 운영 규칙

- Class Diagram과 실제 구현 사이에 차이가 생기면 항상 이 파일(`docs/Extensions.md`)에 누적 기록한다.
- 새로운 클래스, 메소드, DTO 필드, Repository 조회 메소드, 개발 전용 진입점이 추가되면 클래스와 함께 변경 사유를 남긴다.
- Class Diagram 자체를 수정한 경우에도 수정 내용과 이유를 이 파일에 같이 기록한다.
- 도메인 Entity의 속성이나 메소드가 추가되면 별도 섹션으로 강조해서 남긴다.
- 파일명은 현재 프로젝트 기준에 맞춰 `Extensions.md`로 유지한다.

## 기준

- 기준 다이어그램: `images/Implementation_Class_Diagram.puml`
- 작성 목적: 최근 프론트엔드/MVC 검증 흐름과 Progress 화면 확장 과정에서 Class Diagram 밖으로 추가된 메소드를 클래스별로 추적한다.
- 도메인 Entity에는 별도 속성이나 도메인 동작 메소드를 추가하지 않았다.
- `ProgressService`의 외부 공개 메소드는 Class Diagram과 동일하게 `getProgress(userId)` 하나를 유지한다.

## 추가 메소드

| Class | Method | 구분 | 추가 이유 |
| --- | --- | --- | --- |
| `AuthPageController` | `index()` | MVC 화면 라우팅 | `/` 요청을 로그인 화면으로 연결하기 위한 화면 진입점 |
| `AuthPageController` | `login()` | MVC 화면 라우팅 | 로그인 템플릿 렌더링 |
| `AuthPageController` | `register()` | MVC 화면 라우팅 | 회원가입 템플릿 렌더링 |
| `AuthPageController` | `dashboard()` | MVC 화면 라우팅 | 사용자 대시보드 템플릿 렌더링 |
| `AuthPageController` | `progress()` | MVC 화면 라우팅 | Progress 화면 템플릿 렌더링 |
| `DevLoginController` | `devLogin(Model model)` | 개발 전용 진입점 | `local` profile에서 로그인 과정을 건너뛰고 대시보드/Progress 화면을 빠르게 검증하기 위한 `/dev-login` 라우트 |
| `AuthService` | `developmentLogin(String loginId, String password, String name, String email)` | 개발 전용 인증 보조 | 개발용 사용자를 생성 또는 갱신하고 JWT 응답을 만들어 `/dev-login`에서 사용 |
| `CompletionRecordRepository` | `findByTaskUserUserIdAndCanceledFalseAndCompletedDateBetween(Long userId, LocalDate startDate, LocalDate endDate)` | Progress 조회 쿼리 | 최근 7일 완료율, 지난주 대비 수행율, 일별 선 그래프 계산에 필요한 기간별 완료 기록 조회 |
| `FocusSessionRepository` | `findByUserUserIdAndEndAtIsNotNull(Long userId)` | Progress 조회 쿼리 | 종료된 집중 세션만 모아 총 집중 시간을 계산 |
| `ProgressService` | `dailyCompletionRates(LocalDate startDate, int totalTasks, List<CompletionRecord> completions)` | private 계산 보조 | 최근 7일 날짜별 완료율 DTO 목록 생성 |
| `ProgressService` | `rate(int completedCount, int totalTasks)` | private 계산 보조 | 완료 수와 전체 Task 수를 0.1 단위 퍼센트로 변환 |
| `ProgressService` | `roundOneDecimal(double value)` | private 계산 보조 | Progress 응답의 비율 값을 소수점 첫째 자리로 정리 |

## 관련 DTO 확장

아래 항목은 메소드는 아니지만, Progress 화면이 요구한 데이터를 프론트엔드에 전달하기 위해 `ProgressResDto` 응답에 추가된 필드다.

| DTO | Field | 사용 화면 |
| --- | --- | --- |
| `ProgressResDto` | `totalFocusMinutes` | 총 집중 시간 카드 |
| `ProgressResDto` | `completedGoalCount` | 완료한 목표 카드 |
| `ProgressResDto` | `currentWeekCompletionRate` | 이번 주 수행율 계산 |
| `ProgressResDto` | `previousWeekCompletionRate` | 지난주 대비 수행율 계산 |
| `ProgressResDto` | `weeklyCompletionRateDelta` | 지난주 대비 증감 표시 |
| `ProgressResDto` | `dailyCompletionRates` | 최근 7일 완료율 선 그래프 |
| `DailyCompletionRateResDto` | `date`, `label`, `completedCount`, `totalTasks`, `completionRate` | 선 그래프의 날짜별 좌표와 라벨 |

## 유지 원칙

- Class Diagram의 핵심 도메인 모델은 유지한다.
- 화면 구현에 필요한 추가 정보는 Entity 저장 필드가 아니라 DTO와 조회 쿼리에서 계산한다.
- 개발 편의용 `/dev-login`은 `local` profile에만 열어 실제 운영 흐름과 분리한다.

## Reminder Notification Implementation Deviations

### Reason

The public Scheduler, Controller, and Service method surface from `images/Implementation_Class_Diagram.puml` was kept. Reminder scheduler email and in-site notification work still required enum values, SMTP configuration, constructor dependencies, private helpers, a widened `Reminder` column length, and a dashboard hook.

### Public Method Policy

No new public Controller, Scheduler, or Service methods were added. Existing methods were reused:

- `ReminderScheduler.runAvailabilityReminderCheck()`: checks available time and creates in-site reminders only.
- `ReminderScheduler.runDeadlineReminderCheck()`: checks Daily Goal day-end email reminders and Deadline Task offset email reminders.
- `ReminderService.checkAvailabilityReminder(User, LocalDateTime)`: creates `IN_SITE` reminders using the existing availability entry point.
- `ReminderService.checkDeadlineReminder(User, LocalDateTime)`: creates Daily Goal and Deadline Task email reminders using the existing deadline entry point.
- `ReminderService.sendReminder(Reminder)`: routes `EMAIL` through SMTP and `IN_SITE` through local status/history updates.

### Enum Values Added

- `DAILY_GOAL_DAY_END_ONE_HOUR`
- `DAILY_GOAL_DAY_END_THIRTY_MINUTES`
- `DEADLINE_ONE_HOUR`
- `DEADLINE_THIRTY_MINUTES`

### Implementation Dependencies Added To ReminderService

- `DailyGoalRepository`
- `ObjectProvider<JavaMailSender>`
- `app.notification.email.enabled`
- `app.notification.email.from`

### Entity Mapping Adjustment

- `Reminder.reminderType` column length was widened from 30 to 40 because the longest new enum value is longer than 30.

### Private Helper Note

Private helpers added inside `ReminderService`:

- `unfinishedWorkForAvailability`
- `createDailyGoalEmailReminders`
- `createDeadlineTaskEmailReminders`
- `createEmailReminderIfDue`
- `sendEmailReminder`
- `failReminder`
- `isInTargetWindow`
- `hasTaskReminderAt`
- `emailSubject`
- `addIfNotNull`

### Frontend Hook Added

- `dashboard/index.html` adds `data-reminder-toast-region` for the accessible in-site reminder toast region.
