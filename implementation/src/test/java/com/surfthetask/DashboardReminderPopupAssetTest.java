package com.surfthetask;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardReminderPopupAssetTest {

    @Test
    void deadlineEmailReminderTypesAreIncludedInDashboardPopupCandidates() throws Exception {
        String dashboardJs = Files.readString(Path.of("src/main/resources/static/js/dashboard.js"));
        String popupPredicate = sectionBetween(dashboardJs, "function isPopupReminder", "function showReminderToast");
        String deadlinePredicate = sectionBetween(
                dashboardJs,
                "function isDeadlinePopupReminderType",
                "function showReminderToast"
        );

        assertThat(popupPredicate)
                .contains("reminder.status === \"SENT\"")
                .contains("reminder.channel === \"IN_SITE\"")
                .contains("isDeadlinePopupReminderType(reminder.reminderType)");
        assertThat(deadlinePredicate)
                .contains("DEADLINE_ONE_HOUR")
                .contains("DEADLINE_THIRTY_MINUTES")
                .contains("DAILY_GOAL_DAY_END_ONE_HOUR")
                .contains("DAILY_GOAL_DAY_END_THIRTY_MINUTES");
    }

    private String sectionBetween(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());

        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return source.substring(start, end);
    }
}
