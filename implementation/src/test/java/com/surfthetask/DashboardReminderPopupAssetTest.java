package com.surfthetask;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardReminderPopupAssetTest {

    @Test
    void deadlineEmailReminderTypesAreIncludedInDashboardPopupCandidates() throws Exception {
        String dashboardJs = Files.readString(Path.of("src/main/resources/static/js/dashboard.js"));

        assertThat(dashboardJs)
                .contains("function isPopupReminder")
                .contains("DEADLINE_ONE_HOUR")
                .contains("DEADLINE_THIRTY_MINUTES")
                .contains("DAILY_GOAL_DAY_END_ONE_HOUR")
                .contains("DAILY_GOAL_DAY_END_THIRTY_MINUTES");
    }
}
