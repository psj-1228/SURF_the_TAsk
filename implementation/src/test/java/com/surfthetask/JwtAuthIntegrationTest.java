package com.surfthetask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthIntegrationTest {

    private static final String LOGIN_PREFIX = "jwt_smoke_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupJwtSmokeData();
    }

    @AfterEach
    void tearDown() {
        cleanupJwtSmokeData();
    }

    @Test
    void currentUserTaskApiRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtTokenScopesTaskListToAuthenticatedUser() throws Exception {
        AuthenticatedUser userA = registerAndLogin("a");
        AuthenticatedUser userB = registerAndLogin("b");

        createDailyGoal(userA.token(), "A only task");
        createDailyGoal(userB.token(), "B only task");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(userA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("A only task"))
                .andExpect(jsonPath("$[0].title").value(not(containsString("B only task"))));
    }

    @Test
    void legacyUserIdTaskUrlIsNotAvailable() throws Exception {
        AuthenticatedUser victim = registerAndLogin("victim");
        AuthenticatedUser attacker = registerAndLogin("attacker");

        mockMvc.perform(post("/api/users/{userId}/tasks/daily-goals", victim.userId())
                        .header("Authorization", bearer(attacker.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Unsafe path task",
                                "description", "must not be created by path user id",
                                "estimatedMinutes", 10,
                                "importance", 3,
                                "targetCountPerDay", 1
                        ))))
                .andExpect(status().isNotFound());
    }

    private AuthenticatedUser registerAndLogin(String suffix) throws Exception {
        String loginId = LOGIN_PREFIX + suffix;
        String password = "test1234";

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "loginId", loginId,
                                "password", password,
                                "name", "JWT " + suffix,
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
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode login = objectMapper.readTree(loginBody);
        return new AuthenticatedUser(userId, login.get("token").asText());
    }

    private void createDailyGoal(String token, String title) throws Exception {
        mockMvc.perform(post("/api/tasks/daily-goals")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", title,
                                "description", "jwt scoped task",
                                "estimatedMinutes", 25,
                                "importance", 4,
                                "targetCountPerDay", 1
                        ))))
                .andExpect(status().isCreated());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanupJwtSmokeData() {
        jdbcTemplate.execute("""
                DELETE rh FROM reminder_histories rh
                JOIN reminders r ON rh.reminder_id = r.reminder_id
                JOIN users u ON r.user_id = u.user_id
                WHERE u.login_id LIKE 'jwt_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE r FROM reminders r
                JOIN users u ON r.user_id = u.user_id
                WHERE u.login_id LIKE 'jwt_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE fs FROM focus_sessions fs
                JOIN users u ON fs.user_id = u.user_id
                WHERE u.login_id LIKE 'jwt_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE cr FROM completion_records cr
                JOIN tasks t ON cr.task_id = t.task_id
                JOIN users u ON t.user_id = u.user_id
                WHERE u.login_id LIKE 'jwt_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE t FROM tasks t
                JOIN users u ON t.user_id = u.user_id
                WHERE u.login_id LIKE 'jwt_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE ps FROM personal_schedules ps
                JOIN users u ON ps.user_id = u.user_id
                WHERE u.login_id LIKE 'jwt_smoke_%'
                """);
        jdbcTemplate.execute("""
                DELETE np FROM notification_preferences np
                JOIN users u ON np.user_id = u.user_id
                WHERE u.login_id LIKE 'jwt_smoke_%'
                """);
        jdbcTemplate.execute("DELETE FROM users WHERE login_id LIKE 'jwt_smoke_%'");
    }

    private record AuthenticatedUser(long userId, String token) {
    }
}
