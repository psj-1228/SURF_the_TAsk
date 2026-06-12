package com.surfthetask;

import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.entity.User;
import com.surfthetask.domain.enums.TaskStatus;
import com.surfthetask.repository.DailyGoalRepository;
import com.surfthetask.service.DailyGoalRolloverService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyGoalRolloverServiceTest {

    @Mock
    private DailyGoalRepository dailyGoalRepository;

    private DailyGoalRolloverService rolloverService;
    private User user;

    @BeforeEach
    void setUp() {
        rolloverService = new DailyGoalRolloverService(dailyGoalRepository);
        user = new User("daily_rollover_user", "hash", "Daily User", "daily@example.com");
    }

    @Test
    void rolloverReturnsYesterdayCompletedGoalToTodoAndKeepsStreak() {
        LocalDate today = LocalDate.of(2026, 6, 13);
        DailyGoal goal = completedDailyGoal("Study algorithms", today.minusDays(1), 2);

        when(dailyGoalRepository.findByStatusInOrLastCompletedDateBefore(
                eq(List.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS)),
                eq(today)
        )).thenReturn(List.of(goal));

        rolloverService.rollOver(today);

        assertThat(goal.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(goal.getCurrentStreak()).isEqualTo(2);
        assertThat(goal.getLastCompletedDate()).isEqualTo(today.minusDays(1));
    }

    @Test
    void rolloverReturnsMissedGoalToTodoAndResetsStreak() {
        LocalDate today = LocalDate.of(2026, 6, 13);
        DailyGoal goal = completedDailyGoal("Read paper", today.minusDays(2), 4);

        when(dailyGoalRepository.findByStatusInOrLastCompletedDateBefore(
                eq(List.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS)),
                eq(today)
        )).thenReturn(List.of(goal));

        rolloverService.rollOver(today);

        assertThat(goal.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(goal.getCurrentStreak()).isZero();
        assertThat(goal.getLastCompletedDate()).isEqualTo(today.minusDays(2));
    }

    @Test
    void rolloverKeepsTodayCompletedGoalDone() {
        LocalDate today = LocalDate.of(2026, 6, 13);
        DailyGoal goal = completedDailyGoal("Practice English", today, 1);

        when(dailyGoalRepository.findByStatusInOrLastCompletedDateBefore(
                eq(List.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS)),
                eq(today)
        )).thenReturn(List.of(goal));

        rolloverService.rollOver(today);

        assertThat(goal.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(goal.getCurrentStreak()).isEqualTo(1);
        assertThat(goal.getLastCompletedDate()).isEqualTo(today);
    }

    @Test
    void rolloverClearsStaleInProgressGoalAfterMissedDay() {
        LocalDate today = LocalDate.of(2026, 6, 13);
        DailyGoal goal = dailyGoal("Stretch");
        goal.recordCompletion(today.minusDays(3));
        goal.changeStatus(TaskStatus.IN_PROGRESS);

        when(dailyGoalRepository.findByStatusInOrLastCompletedDateBefore(
                eq(List.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS)),
                eq(today)
        )).thenReturn(List.of(goal));

        rolloverService.rollOver(today);

        assertThat(goal.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(goal.getCurrentStreak()).isZero();
        assertThat(goal.getLastCompletedDate()).isEqualTo(today.minusDays(3));
    }

    private DailyGoal completedDailyGoal(String title, LocalDate lastCompletedDate, int expectedStreak) {
        DailyGoal goal = dailyGoal(title);
        LocalDate startDate = lastCompletedDate.minusDays(expectedStreak - 1L);
        for (int day = 0; day < expectedStreak; day++) {
            goal.recordCompletion(startDate.plusDays(day));
        }
        goal.markComplete(LocalDateTime.of(lastCompletedDate, java.time.LocalTime.NOON));
        return goal;
    }

    private DailyGoal dailyGoal(String title) {
        return new DailyGoal(user, title, "", 30, 4, 1);
    }
}
