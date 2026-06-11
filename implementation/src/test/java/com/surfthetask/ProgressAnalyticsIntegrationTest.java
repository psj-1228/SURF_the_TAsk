package com.surfthetask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProgressAnalyticsIntegrationTest {

    private static final String LOGIN_PREFIX = "progress_smoke_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupProgressSmokeData();
    }

    @AfterEach
    void tearDown() {
        cleanupProgressSmokeData();
    }

    @Test
    void progressIncludesWeeklyComparisonFocusMinutesAndDailyRates() throws Exception {
        AuthenticatedUser user = registerAndLogin("analytics");
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        long doneDaily = insertDailyGoal(user.userId(), "Done daily", "DONE", 14, today, now);
        long doneDeadline = insertDeadlineTask(user.userId(), "Done deadline", "DONE", now);
        insertDailyGoal(user.userId(), "Todo daily", "TODO", 3, today.minusDays(2), now);
        insertDeadlineTask(user.userId(), "Todo deadline", "TODO", now);

        insertCompletion(doneDaily, today.minusDays(6));
        insertCompletion(doneDaily, today.minusDays(3));
        insertCompletion(doneDaily, today);
        insertCompletion(doneDeadline, today);
        insertCompletion(doneDaily, today.minusDays(8));

        insertFocusSession(user.userId(), doneDaily, now.minusHours(4), now.minusHours(2).minusMinutes(50));
        insertFocusSession(user.userId(), doneDeadline, now.minusHours(2), now.minusHours(1).minusMinutes(15));

        mockMvc.perform(get("/api/progress")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionRate").value(50.0))
                .andExpect(jsonPath("$.bestDailyGoalStreak").value(14))
                .andExpect(jsonPath("$.totalFocusMinutes").value(115))
                .andExpect(jsonPath("$.completedGoalCount").value(2))
                .andExpect(jsonPath("$.todayCompletedDailyGoals").value(1))
                .andExpect(jsonPath("$.todayCompletedDeadlineTasks").value(1))
                .andExpect(jsonPath("$.todayCompletedTasks").value(2))
                .andExpect(jsonPath("$.todayCompletionRate").value(50.0))
                .andExpect(jsonPath("$.currentWeekCompletionRate").value(100.0))
                .andExpect(jsonPath("$.previousWeekCompletionRate").value(25.0))
                .andExpect(jsonPath("$.weeklyCompletionRateDelta").value(75.0))
                .andExpect(jsonPath("$.dailyCompletionRates.length()").value(7))
                .andExpect(jsonPath("$.dailyCompletionRates[0].date").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.dailyCompletionRates[0].completionRate").value(25.0))
                .andExpect(jsonPath("$.dailyCompletionRates[3].date").value(today.minusDays(3).toString()))
                .andExpect(jsonPath("$.dailyCompletionRates[3].completionRate").value(25.0))
                .andExpect(jsonPath("$.dailyCompletionRates[6].date").value(today.toString()))
                .andExpect(jsonPath("$.dailyCompletionRates[6].completionRate").value(50.0));
    }

    @Test
    void todayDailyGoalAchievementCountsOnlyCurrentDoneDailyGoalsOnce() throws Exception {
        AuthenticatedUser user = registerAndLogin("daily_cap");
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        long doneDaily = insertDailyGoal(user.userId(), "Still done daily", "DONE", 1, today, now);
        long revertedDaily = insertDailyGoal(user.userId(), "Reverted daily", "TODO", 1, today, now);

        insertCompletion(doneDaily, today);
        insertCompletion(doneDaily, today);
        insertCompletion(revertedDaily, today);

        mockMvc.perform(get("/api/progress")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(2))
                .andExpect(jsonPath("$.doneTasks").value(1))
                .andExpect(jsonPath("$.todayCompletedDailyGoals").value(1))
                .andExpect(jsonPath("$.todayCompletedDeadlineTasks").value(0))
                .andExpect(jsonPath("$.todayCompletedTasks").value(1))
                .andExpect(jsonPath("$.todayCompletionRate").value(50.0))
                .andExpect(jsonPath("$.dailyCompletionRates[6].completedCount").value(1))
                .andExpect(jsonPath("$.dailyCompletionRates[6].completionRate").value(50.0));
    }

    private AuthenticatedUser registerAndLogin(String suffix) throws Exception {
        String loginId = LOGIN_PREFIX + suffix;
        String password = "test1234";

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "loginId", loginId,
                                "password", password,
                                "name", "Progress " + suffix,
                                "email", loginId + "@example.com"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long userId = objectMapper.readTree(registerBody).get("userId").asLong();

        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "loginId", loginId,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode login = objectMapper.readTree(loginBody);
        return new AuthenticatedUser(userId, login.get("token").asText());
    }

    private long insertDailyGoal(
            long userId,
            String title,
            String status,
            int currentStreak,
            LocalDate lastCompletedDate,
            LocalDateTime now
    ) {
        return insertTask("""
                INSERT INTO tasks (
                    task_type, user_id, title, description, estimated_minutes, importance, status,
                    target_count_per_day, current_streak, last_completed_date, created_at, updated_at
                )
                VALUES ('DAILY_GOAL', ?, ?, 'progress smoke', 30, 4, ?, 1, ?, ?, ?, ?)
                """, userId, title, status, currentStreak, lastCompletedDate, now, now);
    }

    private long insertDeadlineTask(long userId, String title, String status, LocalDateTime now) {
        return insertTask("""
                INSERT INTO tasks (
                    task_type, user_id, title, description, estimated_minutes, importance, status,
                    deadline_at, warning_threshold_hours, created_at, updated_at
                )
                VALUES ('DEADLINE_TASK', ?, ?, 'progress smoke', 45, 5, ?, ?, 24, ?, ?)
                """, userId, title, status, now.plusDays(3), now, now);
    }

    private long insertTask(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < args.length; index++) {
                statement.setObject(index + 1, args[index]);
            }
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertCompletion(long taskId, LocalDate completedDate) {
        jdbcTemplate.update("""
                INSERT INTO completion_records (task_id, completed_at, completed_date, is_canceled)
                VALUES (?, ?, ?, false)
                """, taskId, completedDate.atTime(10, 0), completedDate);
    }

    private void insertFocusSession(long userId, long taskId, LocalDateTime startAt, LocalDateTime endAt) {
        jdbcTemplate.update("""
                INSERT INTO focus_sessions (user_id, task_id, start_at, end_at, is_active, actual_finished)
                VALUES (?, ?, ?, ?, false, true)
                """, userId, taskId, startAt, endAt);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanupProgressSmokeData() {
        jdbcTemplate.execute("""
                DELETE rh FROM reminder_histories rh
                JOIN reminders r ON rh.reminder_id = r.reminder_id
                JOIN users u ON r.user_id = u.user_id
                WHERE u.login_id LIKE 'progress_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE r FROM reminders r
                JOIN users u ON r.user_id = u.user_id
                WHERE u.login_id LIKE 'progress_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE fs FROM focus_sessions fs
                JOIN users u ON fs.user_id = u.user_id
                WHERE u.login_id LIKE 'progress_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE cr FROM completion_records cr
                JOIN tasks t ON cr.task_id = t.task_id
                JOIN users u ON t.user_id = u.user_id
                WHERE u.login_id LIKE 'progress_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE t FROM tasks t
                JOIN users u ON t.user_id = u.user_id
                WHERE u.login_id LIKE 'progress_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE ps FROM personal_schedules ps
                JOIN users u ON ps.user_id = u.user_id
                WHERE u.login_id LIKE 'progress_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE np FROM notification_preferences np
                JOIN users u ON np.user_id = u.user_id
                WHERE u.login_id LIKE 'progress_smoke_%'
                """);
        jdbcTemplate.execute("DELETE FROM users WHERE login_id LIKE 'progress_smoke_%'");
    }

    private record AuthenticatedUser(long userId, String token) {
    }
}
