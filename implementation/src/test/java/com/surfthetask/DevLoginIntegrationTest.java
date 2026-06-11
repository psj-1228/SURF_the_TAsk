package com.surfthetask;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DevLoginIntegrationTest {

    private static final String DEV_LOGIN_ID = "surf_dev";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupDevUser();
    }

    @AfterEach
    void tearDown() {
        cleanupDevUser();
    }

    @Test
    void devLoginSeedsLocalStorageAndRedirectsToDashboard() throws Exception {
        String html = mockMvc.perform(get("/dev-login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("localStorage.setItem(\"surfUser\"")))
                .andExpect(content().string(containsString("window.location.replace(\"/dashboard\")")))
                .andExpect(content().string(containsString(DEV_LOGIN_ID)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(tokenFrom(html)).isNotBlank();
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE login_id = ?",
                Integer.class,
                DEV_LOGIN_ID
        );
        assertThat(userCount).isEqualTo(1);
    }

    private String tokenFrom(String html) {
        Matcher matcher = TOKEN_PATTERN.matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private void cleanupDevUser() {
        jdbcTemplate.update("""
                DELETE rh FROM reminder_histories rh
                JOIN reminders r ON rh.reminder_id = r.reminder_id
                JOIN users u ON r.user_id = u.user_id
                WHERE u.login_id = ?
                """, DEV_LOGIN_ID);
        jdbcTemplate.update("""
                DELETE r FROM reminders r
                JOIN users u ON r.user_id = u.user_id
                WHERE u.login_id = ?
                """, DEV_LOGIN_ID);
        jdbcTemplate.update("""
                DELETE fs FROM focus_sessions fs
                JOIN users u ON fs.user_id = u.user_id
                WHERE u.login_id = ?
                """, DEV_LOGIN_ID);
        jdbcTemplate.update("""
                DELETE cr FROM completion_records cr
                JOIN tasks t ON cr.task_id = t.task_id
                JOIN users u ON t.user_id = u.user_id
                WHERE u.login_id = ?
                """, DEV_LOGIN_ID);
        jdbcTemplate.update("""
                DELETE t FROM tasks t
                JOIN users u ON t.user_id = u.user_id
                WHERE u.login_id = ?
                """, DEV_LOGIN_ID);
        jdbcTemplate.update("""
                DELETE ps FROM personal_schedules ps
                JOIN users u ON ps.user_id = u.user_id
                WHERE u.login_id = ?
                """, DEV_LOGIN_ID);
        jdbcTemplate.update("""
                DELETE np FROM notification_preferences np
                JOIN users u ON np.user_id = u.user_id
                WHERE u.login_id = ?
                """, DEV_LOGIN_ID);
        jdbcTemplate.update("DELETE FROM users WHERE login_id = ?", DEV_LOGIN_ID);
    }
}
