# Dashboard Today Achievements Wave Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show today's completed Daily Goals and deadline Tasks in the dashboard wave panel with shell, conch, and illustrated wave visuals.

**Architecture:** Add today's split completion metrics to the existing `/api/progress` response, then render those fields in the current dashboard `wave-panel`. Keep all existing cumulative progress fields intact and scope frontend edits to `dashboard/index.html`, `dashboard.js`, and `dashboard.css`.

**Tech Stack:** Spring Boot, Spring MVC integration tests, Java records, Thymeleaf/static HTML, vanilla JavaScript, CSS illustrations.

---

### Task 1: Backend Today Completion Metrics

**Files:**
- Modify: `implementation/src/test/java/com/surfthetask/ProgressAnalyticsIntegrationTest.java`
- Modify: `implementation/src/main/java/com/surfthetask/dto/response/ProgressResDto.java`
- Modify: `implementation/src/main/java/com/surfthetask/service/ProgressService.java`

- [ ] **Step 1: Write the failing test**

Add expectations to the existing progress analytics integration test:

```java
.andExpect(jsonPath("$.todayCompletedDailyGoals").value(1))
.andExpect(jsonPath("$.todayCompletedDeadlineTasks").value(1))
.andExpect(jsonPath("$.todayCompletedTasks").value(2))
.andExpect(jsonPath("$.todayCompletionRate").value(50.0))
```

The fixture already inserts one Daily Goal completion and one deadline Task completion for `LocalDate.now()`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.surfthetask.ProgressAnalyticsIntegrationTest`
Expected: FAIL because the JSON response does not contain the new fields.

- [ ] **Step 3: Write minimal backend implementation**

Add four nullable-safe fields to `ProgressResDto`:

```java
Integer todayCompletedDailyGoals,
Integer todayCompletedDeadlineTasks,
Integer todayCompletedTasks,
Double todayCompletionRate
```

In `ProgressService.getProgress`, count `currentWeekCompletions` with `completedDate.equals(today)` and split by `instanceof DailyGoal`. Pass those counts and `rate(todayCompletedTasks, totalTasks)` to the record constructor.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests com.surfthetask.ProgressAnalyticsIntegrationTest`
Expected: PASS.

### Task 2: Dashboard Wave Panel Markup and Rendering

**Files:**
- Modify: `implementation/src/main/resources/templates/dashboard/index.html`
- Modify: `implementation/src/main/resources/static/js/dashboard.js`
- Modify: `implementation/src/main/resources/static/css/dashboard.css`
- Test: `implementation/src/test/java/com/surfthetask/MvcPageRenderingTest.java`

- [ ] **Step 1: Write the failing rendering test**

Add dashboard assertions for:

```java
content().string(containsString("data-today-daily-goal-count"))
content().string(containsString("data-today-deadline-task-count"))
content().string(containsString("achievement-shell"))
content().string(containsString("achievement-conch"))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.surfthetask.MvcPageRenderingTest`
Expected: FAIL because the new wave achievement markup is not present.

- [ ] **Step 3: Update dashboard markup and JavaScript**

In the `wave-panel`, change the heading to today's accomplishment and add two achievement cards inside `.wave-visual` with:

```html
data-today-daily-goal-count
data-today-deadline-task-count
achievement-shell
achievement-conch
```

In `dashboard.js`, add refs for the new nodes and update `renderProgress` to use `todayCompletedDailyGoals`, `todayCompletedDeadlineTasks`, `todayCompletedTasks`, and `todayCompletionRate`, falling back to existing aggregate values when the new fields are absent.

- [ ] **Step 4: Run the rendering test to verify it passes**

Run: `./gradlew test --tests com.surfthetask.MvcPageRenderingTest`
Expected: PASS.

### Task 3: Illustrated Wave Styling

**Files:**
- Modify: `implementation/src/main/resources/static/css/dashboard.css`

- [ ] **Step 1: Replace plain progress-wave visuals with illustration styling**

Style `.wave-visual` as a scene with layered waves, foam, highlights, shell/conch badges, and responsive dimensions. Keep existing `--wave-level` animation so progress still changes the scene.

- [ ] **Step 2: Run focused tests after CSS changes**

Run:

```powershell
./gradlew test --tests com.surfthetask.ProgressAnalyticsIntegrationTest --tests com.surfthetask.MvcPageRenderingTest
```

Expected: PASS.

### Task 4: Developer Build and Port 8080 Verification

**Files:**
- No source file changes.

- [ ] **Step 1: Start the developer build on port 8080**

Run from `implementation`:

```powershell
./gradlew bootRun --args='--server.port=8080'
```

- [ ] **Step 2: Verify only the changed dashboard wave panel**

Open `http://localhost:8080/dev/login`, use the development login flow if needed, then inspect `http://localhost:8080/dashboard`. Confirm the wave-panel says today, shows shell/conch achievements, and the illustrated wave scene renders without text overlap.
