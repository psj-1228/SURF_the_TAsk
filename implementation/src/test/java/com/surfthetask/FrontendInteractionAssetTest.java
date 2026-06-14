package com.surfthetask;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendInteractionAssetTest {

    @Test
    void deleteConfirmationsUseAccessibleInAppDialogsInsteadOfNativeConfirm() throws Exception {
        String dashboardJs = Files.readString(Path.of("src/main/resources/static/js/dashboard.js"));
        String scheduleJs = Files.readString(Path.of("src/main/resources/static/js/schedule.js"));

        assertThat(dashboardJs).doesNotContain("window.confirm(");
        assertThat(scheduleJs).doesNotContain("window.confirm(");
        assertThat(dashboardJs).contains("data-confirm-delete-modal");
        assertThat(scheduleJs).contains("data-confirm-delete-modal");
    }

    @Test
    void remindersPageCanUpdateNotificationPreferences() throws Exception {
        Path remindersJsPath = Path.of("src/main/resources/static/js/reminders.js");

        assertThat(remindersJsPath).exists();

        String remindersJs = Files.readString(remindersJsPath);
        assertThat(remindersJs)
                .contains("/api/reminders")
                .contains("/api/notification-preference")
                .contains("\"PATCH\"")
                .contains("function asArray");
    }

    @Test
    void focusModeDefaultsToFirstTaskForPrototypeTimer() throws Exception {
        String focusJs = Files.readString(Path.of("src/main/resources/static/js/focus.js"));

        assertThat(focusJs)
                .contains("selectFirstTaskIfAvailable")
                .contains("refs.taskSelect.value = String(firstTask.taskId)")
                .contains("state.remainingSeconds = state.durationSeconds");
    }

    @Test
    void loginPasswordFindShowsTemporaryToast() throws Exception {
        String authJs = Files.readString(Path.of("src/main/resources/static/js/auth.js"));

        assertThat(authJs)
                .contains("passwordFindLink")
                .contains("개발 진행 중입니다.")
                .contains("window.setTimeout")
                .doesNotContain("social-login-button");
    }

    @Test
    void dashboardOverviewFiltersOnlyActionableTasks() throws Exception {
        String dashboardJs = Files.readString(Path.of("src/main/resources/static/js/dashboard.js"));

        assertThat(dashboardJs)
                .contains("function isActionableTask")
                .contains("function isDeadlineStillOpen")
                .contains("task.status !== \"DONE\"")
                .contains("new Date(task.deadlineAt).getTime() >= Date.now()");
    }

    @Test
    void dashboardAvailabilityBannerOnlyShowsDuringCurrentAvailableSlot() throws Exception {
        String dashboardHtml = Files.readString(Path.of("src/main/resources/templates/dashboard/index.html"));
        String dashboardJs = Files.readString(Path.of("src/main/resources/static/js/dashboard.js"));

        assertThat(dashboardHtml)
                .contains("data-availability-banner aria-label=\"지금 가능한 시간\" hidden");
        assertThat(dashboardJs)
                .contains("availabilityBanner: document.querySelector(\"[data-availability-banner]\")")
                .contains("refs.availabilityBanner.hidden = !isCurrentlyAvailable(slots)")
                .contains("function currentDayOfWeek")
                .contains("function isCurrentlyAvailable");
    }

    @Test
    void dashboardAvailabilityListShowsAllWeekdaySlotsIncludingSunday() throws Exception {
        String dashboardJs = Files.readString(Path.of("src/main/resources/static/js/dashboard.js"));

        assertThat(dashboardJs)
                .contains("slots.slice(0, 7)")
                .contains("SUNDAY: \"일요일\"");
    }

    @Test
    void taskArchiveAssetLoadsAllTasksByType() throws Exception {
        Path archiveJsPath = Path.of("src/main/resources/static/js/task-archive.js");

        assertThat(archiveJsPath).exists();

        String archiveJs = Files.readString(archiveJsPath);
        assertThat(archiveJs)
                .contains("/api/tasks")
                .contains("data-task-archive-page")
                .contains("DAILY_GOAL")
                .contains("DEADLINE_TASK");
    }

    @Test
    void scheduleFormUsesSingleAutomaticColorFiveMinuteTimeStepsAndWeekendOptions() throws Exception {
        String modernCss = Files.readString(Path.of("src/main/resources/static/css/modern.css"));
        String scheduleHtml = Files.readString(Path.of("src/main/resources/templates/schedule/index.html"));
        String scheduleJs = Files.readString(Path.of("src/main/resources/static/js/schedule.js"));
        String scheduleCss = Files.readString(Path.of("src/main/resources/static/css/schedule.css"));

        assertThat(scheduleHtml)
                .contains("modern-shell schedule-shell")
                .contains("side-nav modern-side-nav")
                .doesNotContain("data-prototype-screen=\"schedule\"")
                .contains("value=\"SATURDAY\"")
                .contains("value=\"SUNDAY\"")
                .contains("name=\"startTime\" type=\"time\" min=\"07:00\" max=\"23:30\" step=\"300\"")
                .contains("name=\"endTime\" type=\"time\" min=\"07:30\" max=\"23:59\" step=\"300\"")
                .doesNotContain("schedule-color-field")
                .doesNotContain("data-schedule-color-palette");
        assertThat(scheduleJs)
                .contains("{ value: \"SATURDAY\", label: \"토\" }")
                .contains("{ value: \"SUNDAY\", label: \"일\" }");
        assertThat(scheduleCss).contains("grid-template-columns: 58px repeat(7, minmax(86px, 1fr));");
        assertThat(modernCss)
                .contains("[hidden]")
                .contains("grid-template-columns: 58px repeat(7, minmax(86px, 1fr));")
                .doesNotContain("schedule-color-field")
                .doesNotContain("schedule-color-palette");
    }

    @Test
    void taskArchiveTemplatesMatchSharedModernArchiveAssets() throws Exception {
        String modernCss = Files.readString(Path.of("src/main/resources/static/css/modern.css"));
        String dailyHtml = Files.readString(Path.of("src/main/resources/templates/tasks/daily-goals.html"));
        String deadlineHtml = Files.readString(Path.of("src/main/resources/templates/tasks/deadline-tasks.html"));
        String remindersHtml = Files.readString(Path.of("src/main/resources/templates/reminders/index.html"));
        String archiveJs = Files.readString(Path.of("src/main/resources/static/js/task-archive.js"));

        assertThat(modernCss)
                .contains("body {\n    margin: 0;")
                .contains(".modern-shell")
                .contains(".modern-main");
        assertThat(dailyHtml)
                .contains("modern-shell task-archive-shell")
                .contains("modern-main task-archive-main")
                .contains("task-archive-panel")
                .contains("data-task-archive-page=\"daily\"");
        assertThat(deadlineHtml)
                .contains("modern-shell task-archive-shell")
                .contains("modern-main task-archive-main")
                .contains("task-archive-panel")
                .contains("data-task-archive-page=\"deadline\"");
        assertThat(remindersHtml)
                .contains("modern-shell")
                .contains("side-nav modern-side-nav");
        assertThat(modernCss)
                .contains(".task-archive-main")
                .contains(".task-archive-panel")
                .contains(".task-archive-item")
                .contains(".task-archive-meta");
        assertThat(archiveJs)
                .contains("task-archive-item")
                .contains("task-archive-title")
                .contains("task-archive-meta");
    }

    @Test
    void cleanupPagesDoNotCarryPrototypeOrDuplicateSidebarStyles() throws Exception {
        String focusCss = Files.readString(Path.of("src/main/resources/static/css/focus.css"));
        String progressCss = Files.readString(Path.of("src/main/resources/static/css/progress.css"));
        String modernCss = Files.readString(Path.of("src/main/resources/static/css/modern.css"));
        String focusHtml = Files.readString(Path.of("src/main/resources/templates/focus/index.html"));
        String progressHtml = Files.readString(Path.of("src/main/resources/templates/progress/index.html"));

        assertThat(focusHtml)
                .contains("modern-shell focus-shell")
                .contains("modern-side-nav")
                .doesNotContain("data-prototype-screen=\"focus\"");
        assertThat(progressHtml)
                .doesNotContain("side-nav")
                .doesNotContain("nav-list")
                .doesNotContain("period-tabs")
                .doesNotContain("data-prototype-screen=\"progress\"");
        assertThat(focusCss)
                .doesNotContain(".side-nav")
                .doesNotContain(".nav-list");
        assertThat(progressCss)
                .doesNotContain(".side-nav")
                .doesNotContain(".nav-list");
        assertThat(modernCss)
                .doesNotContain(".focus-shell[data-prototype-screen=\"focus\"]");
    }
}
