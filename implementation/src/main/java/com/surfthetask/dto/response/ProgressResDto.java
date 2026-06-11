package com.surfthetask.dto.response;

import java.util.List;

public record ProgressResDto(
        Long userId,
        Integer totalTasks,
        Integer doneTasks,
        Integer incompleteTasks,
        Double completionRate,
        Integer bestDailyGoalStreak,
        Integer totalFocusMinutes,
        Integer completedGoalCount,
        Double currentWeekCompletionRate,
        Double previousWeekCompletionRate,
        Double weeklyCompletionRateDelta,
        List<DailyCompletionRateResDto> dailyCompletionRates,
        List<TaskResDto> priorityTasks,
        Integer todayCompletedDailyGoals,
        Integer todayCompletedDeadlineTasks,
        Integer todayCompletedTasks,
        Double todayCompletionRate
) {
}
