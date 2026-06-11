# Task CRUD and Progress Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build dashboard-based Daily Goal and Deadline Task CRUD with completion checks, plus a separate progress page that highlights per-Daily-Goal streaks.

**Architecture:** Keep the existing Spring MVC + Thymeleaf + static CSS/vanilla JavaScript structure. Add page-rendering tests for required HTML hooks, then implement the dashboard controls, the `/progress` MVC route, and progress page assets against the existing `/api/**` contract.

**Tech Stack:** Java 17, Spring Boot MVC, Thymeleaf, Spring MockMvc, vanilla JavaScript, CSS.

---

### Task 1: MVC Page Hook Tests

**Files:**
- Create: `implementation/src/test/java/com/surfthetask/MvcPageRenderingTest.java`

- [ ] **Step 1: Write the failing test**

Create `MvcPageRenderingTest` with MockMvc checks for the dashboard CRUD hooks and progress page hooks:

```java
package com.surfthetask;

import com.surfthetask.controller.AuthPageController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthPageController.class)
class MvcPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardRendersTaskCrudHooksAndProgressLink() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-daily-goal-form")))
                .andExpect(content().string(containsString("data-deadline-task-form")))
                .andExpect(content().string(containsString("data-task-workspace")))
                .andExpect(content().string(containsString("href=\"/progress\"")));
    }

    @Test
    void progressPageRendersStreakHooks() throws Exception {
        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-progress-page")))
                .andExpect(content().string(containsString("data-daily-streak-list")))
                .andExpect(content().string(containsString("/js/progress.js")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests com.surfthetask.MvcPageRenderingTest`

Expected: FAIL because dashboard hooks and `/progress` do not exist yet.

### Task 2: Dashboard CRUD Markup

**Files:**
- Modify: `implementation/src/main/resources/templates/dashboard/index.html`

- [ ] **Step 1: Add dashboard create forms and progress link**

Add compact forms with these required attributes:

```html
<section class="task-workspace" data-task-workspace aria-label="Task workspace">
  <form data-daily-goal-form>...</form>
  <form data-deadline-task-form>...</form>
</section>
<a class="progress-link" href="/progress">Progress details</a>
```

Each form must include inputs named to match API DTO fields:

- Daily Goal: `title`, `description`, `estimatedMinutes`, `importance`, `targetCountPerDay`.
- Deadline Task: `title`, `description`, `estimatedMinutes`, `importance`, `deadlineAt`, `warningThresholdHours`.

- [ ] **Step 2: Keep existing dashboard data hooks**

Preserve the existing summary, wave, task list, availability, reminder, and logout data attributes already used by `dashboard.js`.

### Task 3: Dashboard CRUD JavaScript

**Files:**
- Modify: `implementation/src/main/resources/static/js/dashboard.js`

- [ ] **Step 1: Implement API helpers**

Add JSON request helpers that send:

```js
{
  "Authorization": "Bearer " + user.token,
  "Content-Type": "application/json"
}
```

Handle `401` by clearing `surfUser` and redirecting to `/login`.

- [ ] **Step 2: Implement create actions**

Wire:

- `data-daily-goal-form` to `POST /api/tasks/daily-goals`.
- `data-deadline-task-form` to `POST /api/tasks/deadline-tasks`.

On success, reset the form and reload dashboard data.

- [ ] **Step 3: Implement inline edit actions**

Each rendered task card gets an edit form that calls `PUT /api/tasks/{taskId}` with the common fields and the subtype field for the task.

- [ ] **Step 4: Implement completion and delete actions**

Each incomplete task card gets a complete button using:

```http
POST /api/tasks/{taskId}/completion
```

Each task card gets a delete button using:

```http
DELETE /api/tasks/{taskId}
```

After each success, reload tasks and progress.

### Task 4: Progress MVC Page

**Files:**
- Modify: `implementation/src/main/java/com/surfthetask/controller/AuthPageController.java`
- Create: `implementation/src/main/resources/templates/progress/index.html`
- Create: `implementation/src/main/resources/static/js/progress.js`
- Create: `implementation/src/main/resources/static/css/progress.css`

- [ ] **Step 1: Add MVC route**

Add:

```java
@GetMapping("/progress")
public String progress() {
    return "progress/index";
}
```

- [ ] **Step 2: Add progress template**

Template must include:

```html
<main data-progress-page>
  <section data-progress-summary></section>
  <section data-daily-streak-list></section>
  <section data-priority-list></section>
</main>
<script defer src="/js/progress.js"></script>
```

- [ ] **Step 3: Implement progress JavaScript**

Load `GET /api/progress` and `GET /api/tasks`, then render:

- Overall completion rate.
- Done and incomplete task counts.
- Best Daily Goal streak.
- Per-Daily-Goal streak cards sorted by current streak, last completed date, and importance.
- Priority tasks.

### Task 5: Verify and Polish

**Files:**
- Modify as needed: dashboard/progress template, CSS, JS files.

- [ ] **Step 1: Run page hook tests**

Run: `gradle test --tests com.surfthetask.MvcPageRenderingTest`

Expected: PASS.

- [ ] **Step 2: Run broader tests if environment supports DB**

Run: `gradle test`

Expected: PASS if local MySQL is available. If it fails due missing DB connectivity, report the exact environmental failure.

- [ ] **Step 3: Run frontend/MVC app**

Run: `gradle bootRun`

Expected: application starts on `http://localhost:8080`.

- [ ] **Step 4: Browser verify**

Open:

- `http://localhost:8080/dashboard`
- `http://localhost:8080/progress`

Verify no console errors, CRUD hooks render, and unauthenticated JS redirects to `/login` where expected.

---

## Self-Review

- The plan covers dashboard CRUD, completion checks, dashboard progress summary, separate progress page, and Daily Goal streak visibility.
- No placeholder requirements remain.
- API paths match `Implement.md`.
- The first task creates tests that fail before implementation.
