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
    void dashboardRendersTaskCrudHooksAndProgressLink() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-open-task-modal=\"daily\"")))
                .andExpect(content().string(containsString("data-open-task-modal=\"deadline\"")))
                .andExpect(content().string(containsString("data-task-create-form")))
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
                .andExpect(content().string(containsString("data-reminder-toast-region")));
    }

    @Test
    void progressPageRendersStreakHooks() throws Exception {
        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-progress-page")))
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
                .andExpect(content().string(containsString("data-schedule-page")))
                .andExpect(content().string(containsString("data-schedule-grid")))
                .andExpect(content().string(containsString("data-schedule-form")))
                .andExpect(content().string(containsString("href=\"/focus\"")))
                .andExpect(content().string(containsString("/js/schedule.js")));
    }

    @Test
    void focusPageRendersStandaloneFocusModeHooks() throws Exception {
        mockMvc.perform(get("/focus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-focus-page")))
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
}
