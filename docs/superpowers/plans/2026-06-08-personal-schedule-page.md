# Personal Schedule Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the dedicated personal schedule UI and make availability start at 07:00.

**Architecture:** Reuse existing Spring MVC page routing and JWT-backed REST APIs. Add a focused Thymeleaf template plus isolated schedule CSS/JS, and keep server-side availability rules in `ScheduleAnalyzer`.

**Tech Stack:** Spring Boot MVC, Thymeleaf, vanilla JavaScript, CSS, JUnit 5, MockMvc.

---

### Task 1: Page Route And Rendering Test

**Files:**
- Modify: `implementation/src/test/java/com/surfthetask/MvcPageRenderingTest.java`
- Modify: `implementation/src/main/java/com/surfthetask/controller/AuthPageController.java`
- Modify: `implementation/src/main/java/com/surfthetask/config/SecurityConfig.java`

- [ ] Add a MockMvc test that `/schedule` renders the schedule page hooks.
- [ ] Run `./gradlew test --tests com.surfthetask.MvcPageRenderingTest` and confirm the new test fails before implementation.
- [ ] Add both routes to `AuthPageController`.
- [ ] Permit both routes in `SecurityConfig`.
- [ ] Re-run the MockMvc test and confirm it passes.

### Task 2: Availability Rule

**Files:**
- Create: `implementation/src/test/java/com/surfthetask/ScheduleAnalyzerTest.java`
- Modify: `implementation/src/main/java/com/surfthetask/service/ScheduleAnalyzer.java`

- [ ] Add a unit test proving empty schedules create availability from 07:00, not 00:00.
- [ ] Add a unit test proving a schedule ending after 07:00 advances the first availability slot.
- [ ] Run `./gradlew test --tests com.surfthetask.ScheduleAnalyzerTest` and confirm failure before implementation.
- [ ] Change `DAY_START` from `LocalTime.MIN` to `LocalTime.of(7, 0)`.
- [ ] Re-run the unit test and confirm it passes.

### Task 3: Schedule Page UI

**Files:**
- Create: `implementation/src/main/resources/templates/schedule/index.html`
- Create: `implementation/src/main/resources/static/css/schedule.css`
- Create: `implementation/src/main/resources/static/js/schedule.js`
- Modify: `implementation/src/main/resources/templates/dashboard/index.html`
- Modify: `implementation/src/main/resources/templates/progress/index.html`

- [ ] Build the sidebar, topbar, timetable grid, inline registration form, schedule list, and availability summary hooks.
- [ ] Implement schedule loading, creation, deletion, color assignment, grid positioning, and availability rendering in `schedule.js`.
- [ ] Link dashboard and progress sidebars to `/schedule`.
- [ ] Re-run `./gradlew test`.
- [ ] Start the app and visually inspect `/schedule` in the browser.
