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
    void scheduleFormUsesSingleAutomaticColorAndFiveMinuteTimeSteps() throws Exception {
        String modernCss = Files.readString(Path.of("src/main/resources/static/css/modern.css"));
        String scheduleHtml = Files.readString(Path.of("src/main/resources/templates/schedule/index.html"));

        assertThat(scheduleHtml)
                .contains("name=\"startTime\" type=\"time\" min=\"07:00\" max=\"23:30\" step=\"300\"")
                .contains("name=\"endTime\" type=\"time\" min=\"07:30\" max=\"23:59\" step=\"300\"")
                .doesNotContain("schedule-color-field")
                .doesNotContain("data-schedule-color-palette");
        assertThat(modernCss)
                .contains("[hidden]")
                .doesNotContain("schedule-color-field")
                .doesNotContain("schedule-color-palette");
    }
}
