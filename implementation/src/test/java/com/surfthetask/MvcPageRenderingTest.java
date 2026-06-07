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
