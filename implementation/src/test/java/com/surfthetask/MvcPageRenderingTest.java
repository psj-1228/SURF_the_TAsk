package com.surfthetask;

import com.surfthetask.controller.AuthPageController;
import com.surfthetask.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MvcPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void loginPageRendersPrototypeAuthSurface() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-prototype-screen=\"auth\"")))
                .andExpect(content().string(containsString("auth-brand-panel")))
                .andExpect(content().string(containsString(">아이디<")))
                .andExpect(content().string(containsString("password-find-link")))
                .andExpect(content().string(containsString("data-auth-toast")))
                .andExpect(content().string(not(containsString("login-remember"))))
                .andExpect(content().string(not(containsString("social-login-button"))))
                .andExpect(content().string(not(containsString("youremail@university.ac.kr"))));
    }

    @Test
    void dashboardRendersTaskCrudHooksAndProgressLink() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/modern.css")))
                .andExpect(content().string(containsString("data-prototype-screen=\"dashboard\"")))
                .andExpect(content().string(containsString("dashboard-calendar")))
                .andExpect(content().string(containsString("availability-banner")))
                .andExpect(content().string(containsString("href=\"/daily-goals\"")))
                .andExpect(content().string(containsString("href=\"/tasks\"")))
                .andExpect(content().string(containsString("data-open-task-modal=\"daily\"")))
                .andExpect(content().string(containsString("data-open-task-modal=\"deadline\"")))
                .andExpect(content().string(containsString("data-task-create-form")))
                .andExpect(content().string(containsString("data-confirm-delete-modal")))
                .andExpect(content().string(containsString("data-today-daily-goal-count")))
                .andExpect(content().string(containsString("data-today-deadline-task-count")))
                .andExpect(content().string(containsString("achievement-shell")))
                .andExpect(content().string(containsString("achievement-conch")))
                .andExpect(content().string(containsString("beach-visual")))
                .andExpect(content().string(containsString("sand-shore")))
                .andExpect(content().string(not(containsString("data-wave-rate"))))
                .andExpect(content().string(not(containsString("wave-fill"))))
                .andExpect(content().string(containsString("href=\"/focus\"")))
                .andExpect(content().string(containsString("href=\"/progress\"")))
                .andExpect(content().string(containsString("href=\"/reminders\"")))
                .andExpect(content().string(containsString("data-reminder-toast-region")));
    }

    @Test
    void progressPageRendersStreakHooks() throws Exception {
        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/modern.css")))
                .andExpect(content().string(containsString("data-progress-page")))
                .andExpect(content().string(containsString("data-prototype-screen=\"progress\"")))
                .andExpect(content().string(not(containsString("progress-date-control"))))
                .andExpect(content().string(not(containsString(">주간<"))))
                .andExpect(content().string(not(containsString(">월간<"))))
                .andExpect(content().string(not(containsString(">연간<"))))
                .andExpect(content().string(containsString("data-completion-donut")))
                .andExpect(content().string(containsString("data-daily-line-chart")))
                .andExpect(content().string(containsString("data-total-focus-time")))
                .andExpect(content().string(containsString("href=\"/focus\"")))
                .andExpect(content().string(containsString("/js/progress.js")));
    }

    @Test
    void schedulePageRendersTimetableAndInlineFormHooks() throws Exception {
        mockMvc.perform(get("/schedule"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/modern.css")))
                .andExpect(content().string(containsString("data-schedule-page")))
                .andExpect(content().string(containsString("data-prototype-screen=\"schedule\"")))
                .andExpect(content().string(containsString("data-schedule-grid")))
                .andExpect(content().string(containsString("data-schedule-form")))
                .andExpect(content().string(containsString("data-schedule-mode")))
                .andExpect(content().string(containsString("data-schedule-memo")))
                .andExpect(content().string(containsString("name=\"startTime\" type=\"time\" min=\"07:00\" max=\"23:30\" step=\"300\"")))
                .andExpect(content().string(containsString("name=\"endTime\" type=\"time\" min=\"07:30\" max=\"23:59\" step=\"300\"")))
                .andExpect(content().string(not(containsString("schedule-color-field"))))
                .andExpect(content().string(not(containsString("data-schedule-color-palette"))))
                .andExpect(content().string(containsString("data-cancel-schedule-edit")))
                .andExpect(content().string(containsString("data-confirm-delete-modal")))
                .andExpect(content().string(containsString("href=\"/focus\"")))
                .andExpect(content().string(containsString("/js/schedule.js")));
    }

    @Test
    void focusPageRendersStandaloneFocusModeHooks() throws Exception {
        mockMvc.perform(get("/focus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/modern.css")))
                .andExpect(content().string(containsString("data-focus-page")))
                .andExpect(content().string(containsString("data-prototype-screen=\"focus\"")))
                .andExpect(content().string(not(containsString("focus-reminder-card"))))
                .andExpect(content().string(not(containsString("focus-exit-preview"))))
                .andExpect(content().string(not(containsString("sound-dock"))))
                .andExpect(content().string(containsString("data-task-select")))
                .andExpect(content().string(containsString("data-focus-timer")))
                .andExpect(content().string(containsString("data-time-mode-label")))
                .andExpect(content().string(containsString("data-voyage-route")))
                .andExpect(content().string(containsString("data-route-boat")))
                .andExpect(content().string(containsString("data-open-exit-confirm")))
                .andExpect(content().string(containsString("data-exit-confirm-modal")))
                .andExpect(content().string(containsString("/css/focus.css")))
                .andExpect(content().string(containsString("/js/focus.js")));
    }

    @Test
    void dailyGoalsAndTasksPagesRenderAllTaskSurfaces() throws Exception {
        mockMvc.perform(get("/daily-goals"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-task-archive-page=\"daily\"")))
                .andExpect(content().string(containsString("data-task-archive-list")))
                .andExpect(content().string(containsString("/js/task-archive.js")));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-task-archive-page=\"deadline\"")))
                .andExpect(content().string(containsString("data-task-archive-list")))
                .andExpect(content().string(containsString("/js/task-archive.js")));
    }

    @Test
    void remindersPageRendersHistoryAndPreferenceHooks() throws Exception {
        mockMvc.perform(get("/reminders"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/modern.css")))
                .andExpect(content().string(containsString("data-reminders-page")))
                .andExpect(content().string(containsString("data-reminder-history-list")))
                .andExpect(content().string(containsString("data-notification-preference-form")))
                .andExpect(content().string(containsString("data-minimum-interval-minutes")))
                .andExpect(content().string(containsString("/js/reminders.js")));
    }
}
