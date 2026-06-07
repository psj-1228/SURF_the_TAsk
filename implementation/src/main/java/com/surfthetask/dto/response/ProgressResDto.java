package com.surfthetask.dto.response;

import java.util.List;

public record ProgressResDto(
        Long userId,
        Integer totalTasks,
        Integer doneTasks,
        Integer incompleteTasks,
        Double completionRate,
        Integer bestDailyGoalStreak,
        List<TaskResDto> priorityTasks
) {
}
